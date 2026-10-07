package com.optibrain.cloud.adapter.aws;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reference list prices are the only input to resource cost that is not measured;
 * a wrong unit here silently mislabels every volume in an inventory. These tests pin
 * the magnitudes to the published us-east-1 rates so a decimal shift cannot reappear.
 */
class AwsPriceListTest {

    @Test
    @DisplayName("gp2 is billed in dollars per GiB-month, not in fractions of a cent")
    void gp2PriceIsInDollarsPerGibMonth() {
        double price = AwsPriceList.volumePerGibMonth("gp2");
        assertThat(price).isEqualTo(0.10);
        assertThat(100.0 * price).isEqualTo(10.0);
    }

    @Test
    @DisplayName("gp3 is cheaper than gp2 by the published margin")
    void gp3SitsBelowGp2() {
        assertThat(AwsPriceList.volumePerGibMonth("gp3"))
                .isCloseTo(AwsPriceList.volumePerGibMonth("gp2") * 0.8,
                        org.assertj.core.api.Assertions.within(1e-9));
    }

    @Test
    @DisplayName("stored snapshot storage is priced at the standard block rate, not gp3")
    void snapshotStorageUsesStandardRate() {
        assertThat(AwsPriceList.volumePerGibMonth("standard")).isEqualTo(0.05);
        assertThat(AwsPriceList.volumePerGibMonth("standard"))
                .isNotEqualTo(AwsPriceList.volumePerGibMonth("gp3"));
    }

    @Test
    @DisplayName("an unknown volume type defaults to gp3 rather than zero")
    void unknownVolumeTypeFallsBackToGp3() {
        assertThat(AwsPriceList.volumePerGibMonth("not-a-real-type"))
                .isEqualTo(AwsPriceList.volumePerGibMonth("gp3"));
    }
}