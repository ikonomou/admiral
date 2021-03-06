/*
 * Copyright (c) 2018 VMware, Inc. All Rights Reserved.
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with separate copyright notices
 * and license terms. Your use of these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package com.vmware.admiral.test.ui.pages.main;

import org.openqa.selenium.By;

import com.vmware.admiral.test.ui.pages.common.PageLocators;

public class MainPageLocators extends PageLocators {

    public final HomeTabLocators homeTabLocators = new HomeTabLocators();
    public final AdministrationTabLocators administrationTabLocators = new AdministrationTabLocators();

    private final By LOGOUT_BUTTON = By.cssSelector(".header-actions .dropdown .dropdown-item");

    public By logoutButton() {
        return LOGOUT_BUTTON;
    }

}
