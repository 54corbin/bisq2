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

package bisq.desktop.primary.main.content.social.profile;

import bisq.desktop.common.view.View;
import bisq.desktop.layout.Layout;
import bisq.desktop.primary.main.content.social.components.UserProfileDisplay;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserProfileView extends View<VBox, UserProfileModel, UserProfileController> {
    public UserProfileView(UserProfileModel model,
                           UserProfileController controller,
                           UserProfileSelection.View userProfileSelectionView,
                           UserProfileDisplay.View userProfileView,
                           CreateUserProfile.View createUserProfileView) {
        super(new VBox(), model, controller);

        
        root.setPadding(Layout.PADDING);
        root.setSpacing(40);

        root.getChildren().addAll(userProfileSelectionView.getRoot(), 
                userProfileView.getRoot(), 
                createUserProfileView.getRoot());
    }

    @Override
    public void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}
