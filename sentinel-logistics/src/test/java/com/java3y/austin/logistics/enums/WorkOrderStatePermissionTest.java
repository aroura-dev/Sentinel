package com.java3y.austin.logistics.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 工单状态流转权限矩阵单测（P0-3）
 *
 * @author sentinel
 */
class WorkOrderStatePermissionTest {

    @Test
    void adminCanReachAnyState() {
        assertTrue(WorkOrderStatePermission.canChange("ADMIN", WorkOrderState.CLOSED));
        assertTrue(WorkOrderStatePermission.canChange("admin", WorkOrderState.RESOLVED));
    }

    @Test
    void customerServiceCanResolveButNotClose() {
        assertTrue(WorkOrderStatePermission.canChange("CUSTOMER_SERVICE", WorkOrderState.RESOLVED));
        assertFalse(WorkOrderStatePermission.canChange("CUSTOMER_SERVICE", WorkOrderState.CLOSED));
    }

    @Test
    void operatorCanOnlyProcessOrPush() {
        assertTrue(WorkOrderStatePermission.canChange("OPERATOR", WorkOrderState.PROCESSING));
        assertTrue(WorkOrderStatePermission.canChange("OPERATOR", WorkOrderState.PUSHED));
        assertFalse(WorkOrderStatePermission.canChange("OPERATOR", WorkOrderState.RESOLVED));
        assertFalse(WorkOrderStatePermission.canChange("OPERATOR", WorkOrderState.CLOSED));
    }

    @Test
    void financeAndMerchantCannotChangeState() {
        assertFalse(WorkOrderStatePermission.canChange("FINANCE", WorkOrderState.PROCESSING));
        assertFalse(WorkOrderStatePermission.canChange("MERCHANT", WorkOrderState.RESOLVED));
    }

    @Test
    void systemFlowWithoutRoleIsAllowed() {
        assertTrue(WorkOrderStatePermission.canChange(null, WorkOrderState.RESOLVED));
        assertTrue(WorkOrderStatePermission.canChange("", WorkOrderState.CLOSED));
    }

    @Test
    void requireAllowedThrowsForForbiddenTransition() {
        assertThrows(SecurityException.class,
                () -> WorkOrderStatePermission.requireAllowed("OPERATOR", WorkOrderState.RESOLVED));
    }
}