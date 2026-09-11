package com.pumpkings.pkcrates.core.animation.script;

import com.pumpkings.pkcrates.core.animation.AnimationPhase;
import com.pumpkings.pkcrates.core.model.reward.IReward;
import com.pumpkings.pkcrates.core.model.session.CrateSession;
import com.pumpkings.pkcrates.presentation.utils.TextUtil;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/** Runtime for one compiled YAML animation and one opening session. */
public final class ScriptedAnimationPhase implements AnimationPhase {

    private final ScriptAnimationDefinition definition;
    private final Plugin plugin;
    private final Map<String, DisplayState> displays = new HashMap<>();
    private final Map<String, Double> variables = new HashMap<>();
    private Random random;

    public ScriptedAnimationPhase(ScriptAnimationDefinition definition, Plugin plugin) {
        this.definition = definition;
        this.plugin = plugin;
    }

    @Override
    public void onStart(CrateSession session) {
        variables.putAll(definition.variables());
        variables.put("duration", (double) definition.duration());
        variables.put("winner.weight", session.getWonReward() == null ? 0.0 : session.getWonReward().getWeight());
        long sessionSeed = definition.seed() ^ session.getPlayer().getUniqueId().getMostSignificantBits()
                ^ session.getBlockLocation().getBlockX() * 31L ^ session.getBlockLocation().getBlockZ();
        random = new Random(sessionSeed);
        tick(session, 0);
    }

    @Override
    public void onTick(CrateSession session) {
        // CrateTickTask calls onStart before ticksLived advances. Avoid executing tick 0 twice.
        if (session.getTicksLived() > 0) tick(session, session.getTicksLived());
    }

    private void tick(CrateSession session, int tick) {
        variables.put("time", (double) tick);
        variables.put("progress", tick / (double) definition.duration());
        int executed = 0;
        for (ScriptAction action : definition.actions()) {
            if (!action.runsAt(tick)) continue;
            variables.put("action.progress", action.progress(tick));
            if (!ExpressionEngine.condition(action.condition(), variables, random)) continue;
            if (++executed > definition.maxActionsPerTick()) break;
            try {
                execute(action, session);
            } catch (Exception e) {
                plugin.getLogger().warning("Animation '" + definition.id() + "' failed at "
                        + action.source() + ": " + e.getMessage());
            }
        }
    }

    private void execute(ScriptAction action, CrateSession session) {
        switch (action.type()) {
            case EFFECT -> action.effects().forEach(spec -> spec.play(origin(session), session.getPlayer()));
            case SPAWN_DISPLAY -> spawnDisplay(action.arguments(), session);
            case ANIMATE_DISPLAY -> animateDisplay(action.arguments(), session);
            case REMOVE_DISPLAY -> removeDisplay(String.valueOf(action.arguments().get("id")));
            case TITLE -> showTitle(action.arguments(), session);
        }
    }

    private void spawnDisplay(Map<String, Object> args, CrateSession session) {
        String id = String.valueOf(args.getOrDefault("id", "display"));
        if (displays.containsKey(id) || displays.size() >= definition.maxDisplays()) return;

        Location base = origin(session).add(offset(args.get("offset")));
        ItemDisplay display = base.getWorld().spawn(base, ItemDisplay.class);
        display.setBillboard(parseBillboard(args.get("billboard")));
        display.setItemStack(resolveItem(args.get("item"), session));
        display.setTeleportDuration(Math.max(0, Math.min(59, integer(args.get("teleport-duration"), 1))));

        float scale = (float) number(args.getOrDefault("scale", 1.0));
        Transformation transform = display.getTransformation();
        transform.getScale().set(scale, scale, scale);
        display.setTransformation(transform);
        displays.put(id, new DisplayState(display, base.clone()));
    }

    private void animateDisplay(Map<String, Object> args, CrateSession session) {
        String id = String.valueOf(args.getOrDefault("id", args.getOrDefault("display", "display")));
        DisplayState state = displays.get(id);
        if (state == null || !state.display().isValid()) return;

        double rawProgress = variables.getOrDefault("action.progress", 0.0);
        double progress = ease(String.valueOf(args.getOrDefault("easing", "linear")), rawProgress);
        variables.put("motion.progress", progress);
        String motion = String.valueOf(args.getOrDefault("motion", "FLOAT")).toUpperCase(Locale.ROOT);
        double radius = number(args.getOrDefault("radius", 1.0));
        double height = number(args.getOrDefault("height", 1.5));
        double rotations = number(args.getOrDefault("rotations", 1.0));
        double angle = rotations * Math.PI * 2.0 * progress;

        Location target = state.origin().clone();
        switch (motion) {
            case "ORBIT" -> target.add(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
            case "SPIRAL", "HELIX" -> target.add(Math.cos(angle) * radius, height * progress, Math.sin(angle) * radius);
            case "RISE" -> target.add(0, height * progress, 0);
            case "FALL" -> target.add(0, height * (1.0 - progress), 0);
            case "BOUNCE" -> target.add(0, Math.abs(Math.sin(progress * Math.PI * rotations)) * height, 0);
            case "FLOAT" -> target.add(0, Math.sin(progress * Math.PI * 2.0 * rotations) * height, 0);
            case "FLY_TO_PLAYER" -> target = lerp(state.origin(), session.getPlayer().getEyeLocation(), progress);
            case "CUSTOM" -> target.add(number(args.get("x")), number(args.get("y")), number(args.get("z")));
            default -> throw new IllegalArgumentException("unknown motion '" + motion + "'");
        }
        state.display().teleport(target);

        Transformation transform = state.display().getTransformation();
        double fromScale = number(args.getOrDefault("scale-from", currentScale(transform)));
        double toScale = number(args.getOrDefault("scale-to", fromScale));
        float scale = (float) (fromScale + (toScale - fromScale) * progress);
        transform.getScale().set(scale, scale, scale);
        if (args.containsKey("spin")) {
            float spin = (float) (number(args.get("spin")) * Math.PI * 2.0 * progress);
            transform.getLeftRotation().rotationY(spin);
        }
        state.display().setTransformation(transform);
    }

    private void showTitle(Map<String, Object> args, CrateSession session) {
        String title = placeholders(String.valueOf(args.getOrDefault("text", "")), session);
        String subtitle = placeholders(String.valueOf(args.getOrDefault("subtitle", "")), session);
        long fadeIn = integer(args.get("fade-in"), 250);
        long stay = integer(args.get("stay"), 1000);
        long fadeOut = integer(args.get("fade-out"), 250);
        session.getPlayer().showTitle(Title.title(TextUtil.parse(title), TextUtil.parse(subtitle),
                Title.Times.times(Duration.ofMillis(fadeIn), Duration.ofMillis(stay), Duration.ofMillis(fadeOut))));
    }

    private String placeholders(String text, CrateSession session) {
        return text.replace("{player}", session.getPlayer().getName())
                .replace("{crate}", session.getCrate().getName())
                .replace("{winner}", session.getWonReward() == null ? "" : session.getWonReward().getId());
    }

    private ItemStack resolveItem(Object source, CrateSession session) {
        String value = String.valueOf(source == null ? "WINNER" : source).toUpperCase(Locale.ROOT);
        IReward reward = session.getWonReward();
        if (value.equals("RANDOM_REWARD") && !session.getCrate().getRewards().isEmpty()) {
            List<IReward> rewards = session.getCrate().getRewards();
            reward = rewards.get(random.nextInt(rewards.size()));
        }
        if ((value.equals("WINNER") || value.equals("WINNING_REWARD") || value.equals("RANDOM_REWARD"))
                && reward != null && reward.getPreviewItem() != null) return reward.getPreviewItem().clone();
        Material material = Material.matchMaterial(value);
        return new ItemStack(material == null ? Material.BARRIER : material);
    }

    private org.bukkit.util.Vector offset(Object value) {
        if (!(value instanceof List<?> list) || list.size() < 3) return new org.bukkit.util.Vector();
        return new org.bukkit.util.Vector(number(list.get(0)), number(list.get(1)), number(list.get(2)));
    }

    private double number(Object value) {
        return ExpressionEngine.evaluate(value, variables, random);
    }

    private static Location origin(CrateSession session) {
        return session.getBlockLocation().clone().add(0.5, 1.0, 0.5);
    }

    private static Location lerp(Location from, Location to, double progress) {
        return from.clone().add(to.toVector().subtract(from.toVector()).multiply(progress));
    }

    private static float currentScale(Transformation transform) {
        Vector3f scale = transform.getScale();
        return scale.x;
    }

    private static int integer(Object value, int fallback) {
        if (value instanceof Number number) return number.intValue();
        try { return value == null ? fallback : Integer.parseInt(String.valueOf(value)); }
        catch (NumberFormatException e) { return fallback; }
    }

    private static Display.Billboard parseBillboard(Object value) {
        try { return Display.Billboard.valueOf(String.valueOf(value == null ? "CENTER" : value).toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { return Display.Billboard.CENTER; }
    }

    private static double ease(String name, double p) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "ease-in", "quad-in" -> p * p;
            case "ease-out", "quad-out" -> 1.0 - (1.0 - p) * (1.0 - p);
            case "ease-in-out" -> p < 0.5 ? 2 * p * p : 1 - Math.pow(-2 * p + 2, 2) / 2;
            case "sine" -> -(Math.cos(Math.PI * p) - 1) / 2;
            case "ease-out-back" -> 1 + 2.70158 * Math.pow(p - 1, 3) + 1.70158 * Math.pow(p - 1, 2);
            case "bounce" -> 1.0 - Math.abs(Math.cos(p * Math.PI * 3.5)) * (1.0 - p);
            default -> p;
        };
    }

    private void removeDisplay(String id) {
        DisplayState state = displays.remove(id);
        if (state != null && state.display().isValid()) state.display().remove();
    }

    @Override
    public boolean isFinished(CrateSession session) {
        return session.getTicksLived() >= definition.duration();
    }

    @Override
    public void onEnd(CrateSession session) {
        displays.values().forEach(state -> { if (state.display().isValid()) state.display().remove(); });
        displays.clear();
    }

    private record DisplayState(ItemDisplay display, Location origin) {}
}
