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

package bisq.desktop.main.content.bisq_easy.trade_guide.security;

import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BisqEasyGuideSecurityController implements Controller {
    @Getter
    private final BisqEasyGuideSecurityView view;

    public BisqEasyGuideSecurityController(ServiceProvider serviceProvider) {
        BisqEasyGuideSecurityModel model = new BisqEasyGuideSecurityModel();
        view = new BisqEasyGuideSecurityView(model, this);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    void onLearnMore() {
        Browser.open("https://bisq.wiki/Bisq_Easy");
    }

    void onBack() {
        Navigation.navigateTo(NavigationTarget.BISQ_EASY_GUIDE_WELCOME);
    }

    void onNext() {
        Navigation.navigateTo(NavigationTarget.BISQ_EASY_GUIDE_PROCESS);
    }
}
