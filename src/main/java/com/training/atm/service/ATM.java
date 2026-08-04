package com.training.atm.service;

import com.training.atm.repository.ATMInfoRepository;

/**
 * Represents the physical ATM machine.
 *
 * DIP fix: depends on {@link ATMInfoRepository} (the narrow metadata-only
 * interface) rather than the concrete ATMConfigRepository that also manages
 * denomination inventory.
 */
public class ATM {
    private final ATMInfoRepository atmInfo;

    public ATM(ATMInfoRepository atmInfo) {
        this.atmInfo = atmInfo;
    }

    public void show(DisplayScreen screen) {
        screen.printDoubleSeparator();
        screen.println(" BANK ATM SYSTEM");
        screen.println(" Location: " + atmInfo.getLocation());
        screen.println(" Branch:   " + atmInfo.getBranchName());
        screen.printDoubleSeparator();
    }
}
