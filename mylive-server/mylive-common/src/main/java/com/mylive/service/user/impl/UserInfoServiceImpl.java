package com.mylive.service.user.impl;

import com.mylive.config.AppProperties;
import com.mylive.constants.Constants;
import com.mylive.enums.UserGenderEnum;
import com.mylive.enums.UserStatusEnum;
import com.mylive.exception.BusinessException;
import com.mylive.infra.jpa.entity.dto.FollowStats;
import com.mylive.infra.jpa.entity.dto.SysSettingDto;
import com.mylive.infra.jpa.entity.dto.TokenInfo;
import com.mylive.infra.jpa.entity.po.UserInfo;
import com.mylive.infra.jpa.entity.po.id.UserFollowId;
import com.mylive.infra.jpa.entity.vo.PaginationResultVo;
import com.mylive.infra.jpa.entity.vo.UserCountInfoVo;
import com.mylive.infra.jpa.entity.vo.UserInfoVo;
import com.mylive.infra.jpa.repository.UserFollowRepository;
import com.mylive.infra.jpa.repository.UserInfoRepository;
import com.mylive.infra.mapstruct.UserInfoMapper;
import com.mylive.infra.redis.RedisComponent;
import com.mylive.response.ResponseCodeEnum;
import com.mylive.service.file.storage.BasicStorageService;
import com.mylive.service.user.UserInfoService;
import com.mylive.utils.StringTools;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserInfoServiceImpl implements UserInfoService {

    private final UserInfoRepository userInfoRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserInfoMapper userInfoMapper;
    private final RedisComponent redisComponent;
    private final UserFollowRepository userFollowRepository;
    private final BasicStorageService basicStorageService;
    private final JavaMailSender javaMailSender;
    private final AppProperties appProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(String email, String nickname, String password, String emailCode) {
        if (!emailCode.equalsIgnoreCase(redisComponent.getEmailCode(email))) {
            throw new BusinessException("Email code is incorrect");
        }
        UserInfo userInfo = userInfoRepository.findByEmailOrNickname(email, nickname);
        if (null != userInfo) {
            if (email.equals(userInfo.getEmail())) {
                redisComponent.cleanEmailCode(email);
                throw new BusinessException(ResponseCodeEnum.EMAIL_ALREADY_EXISTS);
            } else {
                throw new BusinessException(ResponseCodeEnum.NICKNAME_ALREADY_EXISTS);
            }
        }

        userInfo = new UserInfo();
        userInfo.setNickname(nickname);
        userInfo.setEmail(email);
        userInfo.setPassword(passwordEncoder.encode(password));
        userInfo.setStatus(UserStatusEnum.ACTIVE.getStatus());
        userInfo.setGender(UserGenderEnum.SECRECY.getType());
        userInfo.setTheme(Constants.DEFAULT_THEME);

        SysSettingDto sysSettingDto = redisComponent.getSysSettingDto();
        userInfo.setTotalCoinCount(sysSettingDto.getRegisterCoinCount());
        userInfo.setCurrentCoinCount(sysSettingDto.getRegisterCoinCount());
        userInfoRepository.save(userInfo);
        redisComponent.cleanEmailCode(email);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TokenInfo login(String email, String password, String ip) {
        UserInfo userInfo = userInfoRepository.findByEmail(email);
        if (null == userInfo || !passwordEncoder.matches(password, userInfo.getPassword()))
            throw new BusinessException("Incorrect username or password.");
        if (UserStatusEnum.DISABLED.getStatus().equals(userInfo.getStatus()))
            throw new BusinessException("Account has been disabled.");
        userInfo.setLastLoginIp(ip);
        userInfoRepository.save(userInfo);
        return userInfoMapper.toTokenDto(userInfo);
    }

    @Override
    public UserInfoVo getUserDetail(Long targetUserId, Long currentUserId) {
        UserInfo userInfo = userInfoRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ResponseCodeEnum.NOT_FOUND));

        UserInfoVo vo = userInfoMapper.toVo(userInfo);

        UserCountInfoVo cache = getUserCountInfo(targetUserId);
        vo.setFanCount(cache.getFanCount());
        vo.setFollowCount(cache.getFollowCount());

        if (currentUserId == null) {
            vo.setHaveFollowed(false);
        } else {
            boolean haveFollowed = userFollowRepository.existsById(new UserFollowId(currentUserId, targetUserId));
            vo.setHaveFollowed(haveFollowed);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserInfo(UserInfo userInfo, TokenInfo tokenInfo, String token) {
        UserInfo dbUserInfo = userInfoRepository.findByUserId(userInfo.getUserId());

        boolean nicknameChanged = !dbUserInfo.getNickname().equals(userInfo.getNickname());

        if (nicknameChanged && dbUserInfo.getCurrentCoinCount() < Constants.UPDATE_NICKNAME_COIN) {
            throw new BusinessException("Not enough coins to change nickname");
        }

        String oldAvatar = dbUserInfo.getAvatar();
        boolean avatarChanged = !userInfo.getAvatar().equals(oldAvatar);
        boolean needCleanOldAvatar = avatarChanged && oldAvatar != null;

        dbUserInfo.setNickname(userInfo.getNickname());
        dbUserInfo.setGender(userInfo.getGender());
        dbUserInfo.setAvatar(userInfo.getAvatar());

        if (userInfo.getBirthday() != null) {
            dbUserInfo.setBirthday(userInfo.getBirthday());
        }

        if (userInfo.getSchool() != null) {
            dbUserInfo.setSchool(userInfo.getSchool());
        }

        if (userInfo.getProfile() != null) {
            dbUserInfo.setProfile(userInfo.getProfile());
        }

        if (userInfo.getNoticeInfo() != null) {
            dbUserInfo.setNoticeInfo(userInfo.getNoticeInfo());
        }

        userInfoRepository.saveAndFlush(dbUserInfo);

        // 扣币放最后，避免保存实体后覆盖扣币效果
        if (nicknameChanged) {
            int count = userInfoRepository.decrCoinCount(
                    userInfo.getUserId(),
                    Constants.UPDATE_NICKNAME_COIN
            );
            if (count == 0) {
                throw new BusinessException("Not enough coins to change nickname");
            }
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    redisComponent.cleanUserCountInfo(userInfo.getUserId());
                }
            });
        }

        if (nicknameChanged || avatarChanged) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    tokenInfo.setAvatar(userInfo.getAvatar());
                    tokenInfo.setNickname(userInfo.getNickname());
                    redisComponent.updateTokenInfo(token, tokenInfo);
                    if (needCleanOldAvatar) {
                        try {
                            basicStorageService.delete(oldAvatar);
                        } catch (Exception e) {
                            log.error("failed to delete avatar {}", oldAvatar);
                        }
                    }
                }
            });
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTheme(Integer theme, Long userId) {
        userInfoRepository.updateThemeByUserId(theme, userId);
    }

    @Override
    public UserCountInfoVo getUserCountInfo(Long userId) {
        UserCountInfoVo cache = redisComponent.getUserCountInfo(userId);
        if (cache != null) {
            return cache;
        }
        FollowStats followStats = userFollowRepository.countFollowStats(userId);
        Integer coinCount = userInfoRepository.findCoinCountByUserId(userId);

        UserCountInfoVo vo = new UserCountInfoVo(
                (int) followStats.getFollowCount(),
                (int) followStats.getFanCount(),
                coinCount
        );
        redisComponent.saveUserCountInfo(vo, userId);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long userId) {
        userInfoRepository.updateStatusByUserId(userId);
    }

    @Override
    public PaginationResultVo<UserInfo> getUserInfo4Admin(Integer pageNo, Integer pageSize, String nicknameFuzzy, Integer status) {
        if (pageNo == null || pageNo < 1) pageNo = 1;
        if (pageSize == null || pageSize < 1) pageSize = Constants.PAGE_SIZE;
        Pageable pageable = PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Order.desc("createdAt")));
        Page<UserInfo> page = userInfoRepository.findUserInfoPage(
                StringTools.normalizeKeyword(nicknameFuzzy), status, pageable);
        return PaginationResultVo.fromPage(page);
    }

    @Override
    public void sendEmailCode(String email) {
        UserInfo userInfo = userInfoRepository.findByEmail(email);
        if (userInfo != null) {
            throw new BusinessException(ResponseCodeEnum.EMAIL_ALREADY_EXISTS);
        }
        String code = StringTools.getRandomNumber(Constants.EMAIL_CODE_LENGTH);

        SysSettingDto sysSettingsDto = redisComponent.getSysSettingDto();
        String subject = sysSettingsDto.getRegisterEmailTitle();
        String content = String.format(sysSettingsDto.getRegisterEmailContent(), code);

        redisComponent.saveEmailCode(email, code);

        // 发送邮件
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(appProperties.getMail().getFrom());
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(content, true);
            helper.setSentDate(new Date());
            javaMailSender.send(message);
        } catch (Exception e) {
            log.error("邮件发送失败，email={}", email, e);
            throw new BusinessException(ResponseCodeEnum.INTERNAL_ERROR);
        }
    }
}
