package com.pumpkings.pkcrates.infrastructure.config;

public final class Messages {

    public static final String PREFIX = "general.prefix";
    public static final String NO_PERMISSION = "general.no-permission";
    public static final String PLAYER_ONLY = "general.player-only";
    public static final String UNKNOWN_COMMAND = "general.unknown-command";

    // Crates
    public static final String CRATE_CREATED = "crate.created";
    public static final String CRATE_DELETED = "crate.deleted";
    public static final String CRATE_NOT_FOUND = "crate.not-found";
    public static final String CRATE_ALREADY_EXISTS = "crate.already-exists";
    public static final String CRATE_LIST_EMPTY = "crate.list-empty";
    public static final String CRATE_LIST_HEADER = "crate.list-header";
    public static final String CRATE_LIST_FORMAT = "crate.list-format";
    public static final String CRATE_ALREADY_IN_USE = "crate.already-in-use";
    public static final String CRATE_NO_KEYS_CONFIGURED = "crate.no-keys-configured";
    public static final String CRATE_NO_REWARDS = "crate.no-rewards";
    public static final String CRATE_OPENING = "crate.opening";
    public static final String CRATE_CANNOT_BREAK = "crate.cannot-break";

    // Keys
    public static final String KEY_CREATED = "key.created";
    public static final String KEY_DELETED = "key.deleted";
    public static final String KEY_NOT_FOUND = "key.not-found";
    public static final String KEY_ALREADY_EXISTS = "key.already-exists";
    public static final String KEY_GIVEN = "key.given";
    public static final String KEY_MISSING = "key.missing";

    // Editor / Locations
    public static final String LOCATION_SET = "location.set";
    public static final String LOCATION_REMOVED = "location.removed";
    public static final String LOCATION_LOOK_AT_BLOCK = "location.look-at-block";
    public static final String LOCATION_TELEPORT_SUCCESS = "location.teleport-success";
    public static final String LOCATION_TELEPORT_ERROR = "location.teleport-error";

    // Rewards
    public static final String REWARD_ADDED = "reward.added";
    public static final String REWARD_DELETED = "reward.deleted";
    public static final String REWARD_UNIFIED_ONLY = "reward.unified-only";
    public static final String REWARD_COMMAND_ADDED = "reward.command-added";
    public static final String REWARD_COMMANDS_CLEARED = "reward.commands-cleared";
    public static final String REWARD_VISUAL_UPDATED = "reward.visual-updated";
    public static final String REWARD_HOLD_ITEM = "reward.hold-item";
    public static final String REWARD_WON = "reward.won";
    public static final String REWARD_WON_GLOBAL = "reward.won-global";

    // Animations
    public static final String ANIMATION_UPDATED = "animation.updated";

    // Plugins / Admin
    public static final String PLUGIN_RELOADED = "admin.reloaded";
    public static final String PLAYER_NOT_FOUND = "admin.player-not-found";
    public static final String ACTION_CANCELLED = "admin.action-cancelled";
    public static final String PROMPT_CANCEL = "admin.prompt-cancel";
    public static final String PROMPT_NEW_KEY = "admin.prompt-new-key";
    public static final String PROMPT_NEW_CRATE = "admin.prompt-new-crate";
    public static final String PROMPT_RENAME_KEY = "admin.prompt-rename-key";
    public static final String PROMPT_RENAME_CRATE = "admin.prompt-rename-crate";
    public static final String PROMPT_REWARD_COMMAND = "admin.prompt-reward-command";
    public static final String REWARD_NOT_FOUND = "admin.reward-not-found";
    public static final String PROMPT_RARITY = "admin.prompt-rarity";
    public static final String PROMPT_NEW_RARITY = "admin.prompt-new-rarity";
    public static final String RARITY_EXISTS = "admin.rarity-exists";
    public static final String RARITY_CREATED = "admin.rarity-created";
    public static final String RARITY_REMOVED = "admin.rarity-removed";
    public static final String RARITY_NOT_FOUND = "admin.rarity-not-found";
    public static final String RARITY_ASSIGNED = "admin.rarity-assigned";
    public static final String KEY_MATERIAL_UPDATED = "admin.key-material-updated";
    public static final String KEY_NAME_UPDATED = "admin.key-name-updated";
    public static final String PROMPT_NEW_VALUE = "admin.prompt-new-value";
    public static final String INVALID_NUMBER = "admin.invalid-number";

    // Claim
    public static final String CLAIM_OPENED              = "claim.opened";
    public static final String CLAIM_EMPTY               = "claim.empty";
    public static final String CLAIM_REWARD_CLAIMED      = "claim.reward-claimed";
    public static final String CLAIM_ALL_CLAIMED         = "claim.all-claimed";
    public static final String CLAIM_NOTHING_TO_CLAIM    = "claim.nothing-to-claim";
    public static final String CLAIM_INVENTORY_FULL      = "claim.inventory-full";
    public static final String CLAIM_STORED_NOTIFICATION = "claim.stored-notification";
    public static final String CLAIM_LOGIN_NOTIFICATION  = "claim.login-notification";
    public static final String CLAIM_CLEARED             = "claim.cleared";
    public static final String CLAIM_CLEARED_ALL         = "claim.cleared-all";
    public static final String CLAIM_INSPECTING          = "claim.inspecting";
    public static final String CLAIM_LIMIT_REACHED       = "claim.limit-reached";
    public static final String CLAIM_DISABLED            = "claim.disabled";

    // Mass Opening
    public static final String MASS_OPENING_MENU_OPENED        = "mass-opening.menu-opened";
    public static final String MASS_OPENING_NOT_ENOUGH_KEYS    = "mass-opening.not-enough-keys";
    public static final String MASS_OPENING_STARTED            = "mass-opening.started";
    public static final String MASS_OPENING_COMPLETED          = "mass-opening.completed";
    public static final String MASS_OPENING_LIMIT_REACHED      = "mass-opening.limit-reached";
    public static final String MASS_OPENING_DISABLED           = "mass-opening.disabled";
    public static final String MASS_OPENING_CANCELLED          = "mass-opening.cancelled";
    public static final String MASS_OPENING_NO_PERMISSION      = "mass-opening.no-permission";
    public static final String MASS_OPENING_IN_PROGRESS        = "mass-opening.in-progress";

    private Messages() {}
}
