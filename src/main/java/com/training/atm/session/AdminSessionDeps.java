package com.training.atm.session;

import com.training.atm.repository.*;
import com.training.atm.service.*;

/**
 * Parameter object (Fix #15) that groups the 8 dependencies of
 * {@link AdminSession} into a single cohesive value.
 *
 * See {@link CustomerSessionDeps} for the rationale.
 */
public record AdminSessionDeps(
        DisplayScreen        screen,
        InterestService      interestService,
        ATMInfoRepository    atmInfo,
        DenominationRepository denomRepo,
        AccountRepository    accountRepo,
        CustomerRepository   customerRepo,
        CardRepository       cardRepo,
        AdminLogRepository   adminLog
) {}
