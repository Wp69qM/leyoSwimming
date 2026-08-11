package com.leyoswimming.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.response.ConsentStatusResponse;
import com.leyoswimming.dto.response.CurrentPolicyResponse;
import com.leyoswimming.entity.PrivacyPolicy;
import com.leyoswimming.entity.TermsPolicy;
import com.leyoswimming.entity.UserPrivacyConsent;
import com.leyoswimming.entity.UserTermsConsent;
import com.leyoswimming.enums.ActorType;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.PrivacyPolicyMapper;
import com.leyoswimming.repository.TermsPolicyMapper;
import com.leyoswimming.repository.UserPrivacyConsentMapper;
import com.leyoswimming.repository.UserTermsConsentMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyService {

  private final TermsPolicyMapper termsPolicyMapper;
  private final PrivacyPolicyMapper privacyPolicyMapper;
  private final UserTermsConsentMapper userTermsConsentMapper;
  private final UserPrivacyConsentMapper userPrivacyConsentMapper;

  @Transactional(readOnly = true)
  public CurrentPolicyResponse getCurrentTerms() {
    TermsPolicy policy = findCurrentTerms();
    return new CurrentPolicyResponse(policy.getVersion(), policy.getContent(), policy.getEffectiveAt());
  }

  @Transactional(readOnly = true)
  public CurrentPolicyResponse getCurrentPrivacy() {
    PrivacyPolicy policy = findCurrentPrivacy();
    return new CurrentPolicyResponse(policy.getVersion(), policy.getContent(), policy.getEffectiveAt());
  }

  @Transactional(readOnly = true)
  public ConsentStatusResponse getConsentStatus(ActorType actorType, Long userId) {
    TermsPolicy currentTerms = findCurrentTerms();
    PrivacyPolicy currentPrivacy = findCurrentPrivacy();
    String termsVersion = latestTermsVersion(actorType, userId);
    String privacyVersion = latestPrivacyVersion(actorType, userId);
    boolean needsReconsent =
        !currentTerms.getVersion().equals(termsVersion)
            || !currentPrivacy.getVersion().equals(privacyVersion);
    return new ConsentStatusResponse(
        termsVersion,
        privacyVersion,
        currentTerms.getVersion(),
        currentPrivacy.getVersion(),
        needsReconsent);
  }

  @Transactional
  public void recordConsent(
      ActorType actorType, Long userId, String termsVersion, String privacyVersion) {
    TermsPolicy currentTerms = findCurrentTerms();
    PrivacyPolicy currentPrivacy = findCurrentPrivacy();
    if (!currentTerms.getVersion().equals(termsVersion)
        || !currentPrivacy.getVersion().equals(privacyVersion)) {
      throw new BusinessException(ErrorCode.CONSENT_VERSION_MISMATCH);
    }
    saveTermsConsent(actorType, userId, termsVersion);
    savePrivacyConsent(actorType, userId, privacyVersion);
  }

  public boolean hasAgreedCurrentPolicy(ActorType actorType, Long userId) {
    return !getConsentStatus(actorType, userId).needsReconsent();
  }

  private TermsPolicy findCurrentTerms() {
    TermsPolicy policy =
        termsPolicyMapper
            .selectList(
                new LambdaQueryWrapper<TermsPolicy>()
                    .eq(TermsPolicy::getCurrent, true)
                    .orderByDesc(TermsPolicy::getEffectiveAt)
                    .last("LIMIT 1"))
            .stream()
            .findFirst()
            .orElse(null);
    if (policy == null) {
      throw new BusinessException(ErrorCode.POLICY_NOT_FOUND);
    }
    return policy;
  }

  private PrivacyPolicy findCurrentPrivacy() {
    PrivacyPolicy policy =
        privacyPolicyMapper
            .selectList(
                new LambdaQueryWrapper<PrivacyPolicy>()
                    .eq(PrivacyPolicy::getCurrent, true)
                    .orderByDesc(PrivacyPolicy::getEffectiveAt)
                    .last("LIMIT 1"))
            .stream()
            .findFirst()
            .orElse(null);
    if (policy == null) {
      throw new BusinessException(ErrorCode.POLICY_NOT_FOUND);
    }
    return policy;
  }

  private String latestTermsVersion(ActorType actorType, Long userId) {
    UserTermsConsent consent =
        userTermsConsentMapper
            .selectList(
                new LambdaQueryWrapper<UserTermsConsent>()
                    .eq(UserTermsConsent::getActorType, actorType.getValue())
                    .eq(UserTermsConsent::getUserId, userId)
                    .orderByDesc(UserTermsConsent::getAgreedAt)
                    .last("LIMIT 1"))
            .stream()
            .findFirst()
            .orElse(null);
    return consent == null ? null : consent.getVersion();
  }

  private String latestPrivacyVersion(ActorType actorType, Long userId) {
    UserPrivacyConsent consent =
        userPrivacyConsentMapper
            .selectList(
                new LambdaQueryWrapper<UserPrivacyConsent>()
                    .eq(UserPrivacyConsent::getActorType, actorType.getValue())
                    .eq(UserPrivacyConsent::getUserId, userId)
                    .orderByDesc(UserPrivacyConsent::getAgreedAt)
                    .last("LIMIT 1"))
            .stream()
            .findFirst()
            .orElse(null);
    return consent == null ? null : consent.getVersion();
  }

  private void saveTermsConsent(ActorType actorType, Long userId, String version) {
    if (existsTermsConsent(actorType, userId, version)) {
      return;
    }
    UserTermsConsent consent = new UserTermsConsent();
    consent.setActorType(actorType.getValue());
    consent.setUserId(userId);
    consent.setVersion(version);
    consent.setStatus("agreed");
    consent.setAgreedAt(LocalDateTime.now());
    userTermsConsentMapper.insert(consent);
  }

  private void savePrivacyConsent(ActorType actorType, Long userId, String version) {
    if (existsPrivacyConsent(actorType, userId, version)) {
      return;
    }
    UserPrivacyConsent consent = new UserPrivacyConsent();
    consent.setActorType(actorType.getValue());
    consent.setUserId(userId);
    consent.setVersion(version);
    consent.setStatus("agreed");
    consent.setAgreedAt(LocalDateTime.now());
    userPrivacyConsentMapper.insert(consent);
  }

  private boolean existsTermsConsent(ActorType actorType, Long userId, String version) {
    return
        userTermsConsentMapper
            .selectCount(
                new LambdaQueryWrapper<UserTermsConsent>()
                    .eq(UserTermsConsent::getActorType, actorType.getValue())
                    .eq(UserTermsConsent::getUserId, userId)
                    .eq(UserTermsConsent::getVersion, version))
            > 0;
  }

  private boolean existsPrivacyConsent(ActorType actorType, Long userId, String version) {
    return
        userPrivacyConsentMapper
            .selectCount(
                new LambdaQueryWrapper<UserPrivacyConsent>()
                    .eq(UserPrivacyConsent::getActorType, actorType.getValue())
                    .eq(UserPrivacyConsent::getUserId, userId)
                    .eq(UserPrivacyConsent::getVersion, version))
            > 0;
  }
}
