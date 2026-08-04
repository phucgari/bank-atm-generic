package com.training.atm.service;

import java.util.List;

/** Defines the contract for monthly interest calculation. */
public interface InterestService {
    /**
     * Calculates and credits interest for every account that has not yet
     * received it in the current period.
     *
     * @return a list of human-readable result lines for each account processed
     */
    List<String> calculateInterestForAll();
}
