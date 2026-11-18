package com.gblrod.encurtaai.exception

import com.gblrod.encurtaai.policy.LinkRecoveryPolicy

class LinkRecoveryExpiredException(code: String) :
    RuntimeException("Link with code '$code' can no longer be restored because its ${LinkRecoveryPolicy.RETENTION_PERIOD_DAYS} days recovery period has expired.")