/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.offer.amount.spec;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * No min. amount supported
 */
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class BaseSideFixedAmountSpec extends FixedAmountSpec implements BaseSideAmountSpec {
    public BaseSideFixedAmountSpec(long amount) {
        super(amount);

        verify();
    }

    @Override
    public void verify() {
        super.verify();
    }

    @Override
    public bisq.offer.protobuf.AmountSpec toProto() {
        return getAmountSpecBuilder().setFixedAmountSpec(
                        getFixedAmountSpecBuilder().setBaseSideFixedAmountSpec(
                                bisq.offer.protobuf.BaseSideFixedAmountSpec.newBuilder()))
                .build();
    }

    public static BaseSideFixedAmountSpec fromProto(bisq.offer.protobuf.FixedAmountSpec proto) {
        return new BaseSideFixedAmountSpec(proto.getAmount());
    }
}