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

package bisq.desktop.main.content.reputation.build_reputation.burn.tab2;

import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BurnBsqTab2Controller implements Controller {
    @Getter
    private final BurnBsqTab2View view;

    public BurnBsqTab2Controller(ServiceProvider serviceProvider) {
        BurnBsqTab2Model model = new BurnBsqTab2Model();
        BurnScoreSimulation simulation = new BurnScoreSimulation();
        view = new BurnBsqTab2View(model, this, simulation.getViewRoot());
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    void onBack() {
        Navigation.navigateTo(NavigationTarget.BURN_BSQ_TAB_1);
    }

    void onNext() {
        Navigation.navigateTo(NavigationTarget.BURN_BSQ_TAB_3);
    }

    void onLearnMore() {
        Browser.open("https://bisq.wiki/Reputation");
    }
}
