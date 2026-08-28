package com.pumpkings.pkcrates.infrastructure.config;

import com.pumpkings.pkcrates.core.model.massopening.MassOpeningOption;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class MassOpeningConfig {

    private boolean enabled;
    private List<MassOpeningOption> options;

    public MassOpeningConfig(boolean enabled, List<MassOpeningOption> options) {
        this.enabled = enabled;
        this.options = options == null ? new ArrayList<>() : new ArrayList<>(options);
    }

    public static MassOpeningConfig createDefault(boolean enabled) {
        List<MassOpeningOption> defOptions = new ArrayList<>();
        defOptions.add(new MassOpeningOption(1));
        defOptions.add(new MassOpeningOption(5));
        defOptions.add(new MassOpeningOption(10));
        defOptions.add(new MassOpeningOption(25));
        defOptions.add(new MassOpeningOption(50));
        defOptions.add(new MassOpeningOption(100));
        defOptions.add(new MassOpeningOption(MassOpeningOption.ALL_AMOUNT));
        return new MassOpeningConfig(enabled, defOptions);
    }

    public static MassOpeningConfig parse(ConfigurationSection section) {
        if (section == null) {
            return createDefault(true);
        }

        boolean enabled = section.getBoolean("enabled", true);
        List<MassOpeningOption> options = new ArrayList<>();

        List<Map<?, ?>> optionMaps = section.getMapList("options");
        if (optionMaps != null && !optionMaps.isEmpty()) {
            for (Map<?, ?> map : optionMaps) {
                Object val = map.get("amount");
                if (val != null) {
                    if ("all".equalsIgnoreCase(val.toString())) {
                        options.add(new MassOpeningOption(MassOpeningOption.ALL_AMOUNT));
                    } else {
                        try {
                            int num = Integer.parseInt(val.toString());
                            if (num > 0 || num == MassOpeningOption.ALL_AMOUNT) {
                                options.add(new MassOpeningOption(num));
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }

        // Fallback to string list or integer list if map list was empty
        if (options.isEmpty()) {
            List<String> strList = section.getStringList("options");
            for (String s : strList) {
                if ("all".equalsIgnoreCase(s)) {
                    options.add(new MassOpeningOption(MassOpeningOption.ALL_AMOUNT));
                } else {
                    try {
                        int num = Integer.parseInt(s);
                        if (num > 0 || num == MassOpeningOption.ALL_AMOUNT) {
                            options.add(new MassOpeningOption(num));
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        if (options.isEmpty()) {
            return createDefault(enabled);
        }

        return new MassOpeningConfig(enabled, options);
    }

    public void serialize(ConfigurationSection section) {
        section.set("enabled", enabled);
        List<Map<String, Object>> optList = new ArrayList<>();
        for (MassOpeningOption option : options) {
            if (option.isAll()) {
                optList.add(Map.of("amount", "all"));
            } else {
                optList.add(Map.of("amount", option.getAmount()));
            }
        }
        section.set("options", optList);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<MassOpeningOption> getOptions() {
        return new ArrayList<>(options);
    }

    public void setOptions(List<MassOpeningOption> options) {
        this.options = options == null ? new ArrayList<>() : new ArrayList<>(options);
    }

    public void addOption(MassOpeningOption option) {
        if (option != null && !options.contains(option)) {
            options.add(option);
            sortOptions();
        }
    }

    public void removeOption(MassOpeningOption option) {
        if (option != null) {
            options.remove(option);
        }
    }

    public void sortOptions() {
        options.sort(Comparator.comparingInt(opt -> opt.isAll() ? Integer.MAX_VALUE : opt.getAmount()));
    }
}
