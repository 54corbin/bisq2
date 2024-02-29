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

package bisq.desktop.main.content.settings.preferences;

import bisq.chat.notifications.ChatNotificationService;
import bisq.common.locale.LanguageRepository;
import bisq.common.observable.Pin;
import bisq.common.util.OsUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.settings.ChatNotificationType;
import bisq.settings.CookieKey;
import bisq.settings.DontShowAgainService;
import bisq.settings.SettingsService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class PreferencesController implements Controller {
    @Getter
    private final PreferencesView view;
    private final PreferencesModel model;
    private final SettingsService settingsService;
    private final ChatNotificationService chatNotificationService;

    private Pin chatNotificationTypePin, useAnimationsPin, getPreventStandbyModePin, offerOnlyPin,
            closeMyOfferWhenTakenPin, getSupportedLanguageCodesPin, minRequiredReputationScorePin;
    private Subscription notifyForPreReleasePin, getUseTransientNotificationsPin;

    public PreferencesController(ServiceProvider serviceProvider) {
        settingsService = serviceProvider.getSettingsService();
        chatNotificationService = serviceProvider.getChatService().getChatNotificationService();
        model = new PreferencesModel();
        view = new PreferencesView(model, this);
    }

    @Override
    public void onActivate() {
        model.getLanguageCodes().setAll(LanguageRepository.I18N_CODES);
        model.setSelectedLanguageCode(settingsService.getLanguageCode());
        model.getSupportedLanguageCodes().setAll(LanguageRepository.CODES);

        chatNotificationTypePin = FxBindings.bindBiDir(model.getChatNotificationType())
                .to(settingsService.getChatNotificationType());
        useAnimationsPin = FxBindings.bindBiDir(model.getUseAnimations())
                .to(settingsService.getUseAnimations());
        getPreventStandbyModePin = FxBindings.bindBiDir(model.getPreventStandbyMode())
                .to(settingsService.getPreventStandbyMode());
        minRequiredReputationScorePin = FxBindings.bindBiDir(model.getMinRequiredReputationScore())
                .to(settingsService.getMinRequiredReputationScore());
        offerOnlyPin = FxBindings.bindBiDir(model.getOfferOnly())
                .to(settingsService.getOffersOnly());
        closeMyOfferWhenTakenPin = FxBindings.bindBiDir(model.getCloseMyOfferWhenTaken())
                .to(settingsService.getCloseMyOfferWhenTaken());
        getSupportedLanguageCodesPin = FxBindings.<String, String>bind(model.getSelectedSupportedLanguageCodes())
                .to(settingsService.getSupportedLanguageCodes());

        model.getNotifyForPreRelease().set(settingsService.getCookie().asBoolean(CookieKey.NOTIFY_FOR_PRE_RELEASE).orElse(false));
        notifyForPreReleasePin = EasyBind.subscribe(model.getNotifyForPreRelease(),
                value -> settingsService.setCookie(CookieKey.NOTIFY_FOR_PRE_RELEASE, value));

        // Currently we support transient notifications only for Linux
        if (OsUtils.isLinux()) {
            model.setUseTransientNotificationsVisible(true);
            model.getUseTransientNotifications().set(settingsService.getCookie().asBoolean(CookieKey.USE_TRANSIENT_NOTIFICATIONS).orElse(true));
            getUseTransientNotificationsPin = EasyBind.subscribe(model.getUseTransientNotifications(),
                    value -> settingsService.setCookie(CookieKey.USE_TRANSIENT_NOTIFICATIONS, value));
        }

        model.getSupportedLanguageCodeFilteredList().setPredicate(e -> !model.getSelectedSupportedLanguageCodes().contains(e));
    }

    @Override
    public void onDeactivate() {
        chatNotificationTypePin.unbind();
        useAnimationsPin.unbind();
        minRequiredReputationScorePin.unbind();
        offerOnlyPin.unbind();
        closeMyOfferWhenTakenPin.unbind();
        getPreventStandbyModePin.unbind();
        getSupportedLanguageCodesPin.unbind();
        notifyForPreReleasePin.unsubscribe();
        if (getUseTransientNotificationsPin != null) {
            getUseTransientNotificationsPin.unsubscribe();
        }
    }

    void onSelectLanguage(String languageCode) {
        model.setSelectedLanguageCode(languageCode);
        settingsService.setLanguageCode(languageCode);
        new Popup().feedback(Res.get("settings.preferences.language.restart")).useShutDownButton().show();
    }

    void onResetDontShowAgain() {
        DontShowAgainService.resetDontShowAgain();
    }

    void onClearNotifications() {
        chatNotificationService.consumeAllNotifications();
    }

    void onSetChatNotificationType(ChatNotificationType type) {
        model.getChatNotificationType().set(type);
    }

    String getDisplayLanguage(String languageCode) {
        return LanguageRepository.getDisplayString(languageCode);
    }

    void onSelectSupportedLanguage(String languageCode) {
        if (languageCode != null) {
            model.getSelectedLSupportedLanguageCode().set(languageCode);
        }
    }

    void onAddSupportedLanguage() {
        if (model.getSelectedLSupportedLanguageCode() != null) {
            settingsService.getSupportedLanguageCodes().add(model.getSelectedLSupportedLanguageCode().get());
            model.getSelectedLSupportedLanguageCode().set(null);
            model.getSupportedLanguageCodeFilteredList().setPredicate(e -> !model.getSelectedSupportedLanguageCodes().contains(e));
        }
    }

    void onRemoveSupportedLanguage(String languageCode) {
        if (languageCode != null) {
            settingsService.getSupportedLanguageCodes().remove(languageCode);
            model.getSupportedLanguageCodeFilteredList().setPredicate(e -> !model.getSelectedSupportedLanguageCodes().contains(e));
        }
    }
}
