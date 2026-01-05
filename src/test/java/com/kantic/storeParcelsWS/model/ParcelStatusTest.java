package com.kantic.storeParcelsWS.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ParcelStatus state machine logic.
 */
class ParcelStatusTest {

    @ParameterizedTest
    @CsvSource({
        "PENDING, IN_TRANSIT, true",
        "PENDING, CANCELLED, true",
        "PENDING, DELIVERED, false",
        "PENDING, READY_FOR_PICKUP, false",
        "IN_TRANSIT, READY_FOR_PICKUP, true",
        "IN_TRANSIT, CANCELLED, true",
        "IN_TRANSIT, DELIVERED, false",
        "IN_TRANSIT, PENDING, false",
        "READY_FOR_PICKUP, PICKED_UP, true",
        "READY_FOR_PICKUP, EXPIRED, true",
        "READY_FOR_PICKUP, CANCELLED, true",
        "READY_FOR_PICKUP, DELIVERED, false",
        "READY_FOR_PICKUP, IN_TRANSIT, false",
        "PICKED_UP, DELIVERED, true",
        "PICKED_UP, CANCELLED, true",
        "PICKED_UP, RETURNED, false",
        "EXPIRED, RETURNED, true",
        "EXPIRED, CANCELLED, true",
        "EXPIRED, DELIVERED, false",
        "DELIVERED, CANCELLED, false",
        "DELIVERED, RETURNED, false",
        "RETURNED, CANCELLED, false",
        "RETURNED, PENDING, false",
        "CANCELLED, PENDING, false",
        "CANCELLED, RETURNED, false"
    })
    void shouldValidateStatusTransitions(ParcelStatus from, ParcelStatus to, boolean expected) {
        assertThat(from.canTransitionTo(to)).isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(value = ParcelStatus.class, names = {"DELIVERED", "RETURNED", "CANCELLED"})
    void terminalStatusesShouldNotHaveTransitions(ParcelStatus status) {
        assertThat(status.isTerminal()).isTrue();
        assertThat(status.getAllowedTransitions()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = ParcelStatus.class, names = {"PENDING", "IN_TRANSIT", "READY_FOR_PICKUP", "PICKED_UP", "EXPIRED"})
    void nonTerminalStatusesShouldHaveTransitions(ParcelStatus status) {
        assertThat(status.isTerminal()).isFalse();
        assertThat(status.getAllowedTransitions()).isNotEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = ParcelStatus.class, names = {"CANCELLED", "RETURNED"})
    void shouldBeDeletable(ParcelStatus status) {
        assertThat(status.isDeletable()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = ParcelStatus.class, names = {"PENDING", "IN_TRANSIT", "READY_FOR_PICKUP", "PICKED_UP", "EXPIRED", "DELIVERED"})
    void shouldNotBeDeletable(ParcelStatus status) {
        assertThat(status.isDeletable()).isFalse();
    }

    @Test
    void pendingStatusShouldAllowTransitionToInTransitAndCancelled() {
        assertThat(ParcelStatus.PENDING.getAllowedTransitions())
            .containsExactlyInAnyOrder(ParcelStatus.IN_TRANSIT, ParcelStatus.CANCELLED);
    }

    @Test
    void readyForPickupStatusShouldAllowTransitionToPickedUpExpiredAndCancelled() {
        assertThat(ParcelStatus.READY_FOR_PICKUP.getAllowedTransitions())
            .containsExactlyInAnyOrder(ParcelStatus.PICKED_UP, ParcelStatus.EXPIRED, ParcelStatus.CANCELLED);
    }

    @Test
    void deliveredStatusShouldBeTerminal() {
        assertThat(ParcelStatus.DELIVERED.isTerminal()).isTrue();
        assertThat(ParcelStatus.DELIVERED.getAllowedTransitions()).isEmpty();
        assertThat(ParcelStatus.DELIVERED.canTransitionTo(ParcelStatus.CANCELLED)).isFalse();
    }
}
