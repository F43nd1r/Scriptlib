package com.faendir.lightning_launcher.scriptlib;

import android.support.annotation.IntDef;

/**
 * Matches http://www.lightninglauncher.com/scripting/reference/api/reference/net/pierrox/lightning_launcher/script/api/EventHandler.html
 */
@IntDef({Action.UNSET,
        Action.NOTHING,
        Action.APP_DRAWER,
        Action.ZOOM_FULL_SCALE,
        Action.ZOOM_TO_ORIGIN,
        Action.SWITCH_FULL_SCALE_OR_ORIGIN,
        Action.SHOW_HIDE_STATUS_BAR,
        Action.LAUNCHER_MENU,
        Action.EDIT_LAYOUT,
        Action.CUSTOMIZE_LAUNCHER,
        Action.CUSTOMIZE_PAGE,
        Action.CUSTOMIZE_ITEM,
        Action.ITEM_MENU,
        Action.LAUNCH_ITEM,
        Action.SEARCH,
        Action.SHOW_HIDE_APP_MENU,
        Action.SHOW_HIDE_APP_MENU_STATUS_BAR,
        Action.SHOW_NOTIFICATIONS,
        Action.PREVIOUS_PAGE,
        Action.NEXT_PAGE,
        Action.LAUNCH_APP,
        Action.MOVE_ITEM,
        Action.ADD_ITEM,
        Action.LAUNCH_SHORTCUT,
        Action.SELECT_WALLPAPER,
        Action.GO_HOME,
        Action.GO_HOME_ZOOM_TO_ORIGIN,
        Action.SELECT_DESKTOP_TO_GO_TO,
        Action.RESTART,
        Action.CLOSE_TOPMOST_FOLDER,
        Action.CLOSE_ALL_FOLDERS,
        Action.SEARCH_APP,
        Action.OPEN_FOLDER,
        Action.GO_DESKTOP_POSITION,
        Action.UNLOCK_SCREEN,
        Action.RUN_SCRIPT,
        Action.BACK,
        Action.CUSTOM_MENU,
        Action.USER_MENU,
        Action.WALLPAPER_TAP,
        Action.WALLPAPER_SECONDARY_TAP,
        Action.SET_VARIABLE,
        Action.SHOW_FLOATING_DESKTOP,
        Action.HIDE_FLOATING_DESKTOP,
        Action.OPEN_HIERARCHY_SCREEN
})
public @interface Action {
    int UNSET = 0;
    int NOTHING = 1;
    int APP_DRAWER = 2;
    int ZOOM_FULL_SCALE = 3;
    int ZOOM_TO_ORIGIN = 4;
    int SWITCH_FULL_SCALE_OR_ORIGIN = 5;
    int SHOW_HIDE_STATUS_BAR = 6;
    int LAUNCHER_MENU = 7;
    int EDIT_LAYOUT = 8;
    int CUSTOMIZE_LAUNCHER = 9;
    int CUSTOMIZE_PAGE = 10;
    int CUSTOMIZE_ITEM = 11;
    int ITEM_MENU = 12;
    int LAUNCH_ITEM = 13;
    int SEARCH = 14;
    int SHOW_HIDE_APP_MENU = 15;
    int SHOW_HIDE_APP_MENU_STATUS_BAR = 16;
    int SHOW_NOTIFICATIONS = 17;
    int PREVIOUS_PAGE = 18;
    int NEXT_PAGE = 19;
    int LAUNCH_APP = 20;
    int MOVE_ITEM = 21;
    int ADD_ITEM = 22;
    int LAUNCH_SHORTCUT = 23;
    int SELECT_WALLPAPER = 24;
    int GO_HOME = 25;
    int GO_HOME_ZOOM_TO_ORIGIN = 26;
    int SELECT_DESKTOP_TO_GO_TO = 27;
    int RESTART = 28;
    int CLOSE_TOPMOST_FOLDER = 29;
    int CLOSE_ALL_FOLDERS = 30;
    int SEARCH_APP = 31;
    int OPEN_FOLDER = 32;
    int GO_DESKTOP_POSITION = 33;
    int UNLOCK_SCREEN = 34;
    int RUN_SCRIPT = 35;
    int BACK = 36;
    int CUSTOM_MENU = 37;
    int USER_MENU = 38;
    int WALLPAPER_TAP = 39;
    int WALLPAPER_SECONDARY_TAP = 40;
    int SET_VARIABLE = 41;
    int SHOW_FLOATING_DESKTOP = 42;
    int HIDE_FLOATING_DESKTOP = 43;
    int OPEN_HIERARCHY_SCREEN = 44;
}
