THEN(
    CATCH(
        THEN(
            detectException,
            classifyException,
            createWorkorder,
            notifyChannels,
            IF(isLowConfidence, degradeToTemplate)
        )
    ).DO(degradeToTemplate),
    FINALLY(recordAudit)
)
