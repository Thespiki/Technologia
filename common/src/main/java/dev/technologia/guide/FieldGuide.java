package dev.technologia.guide;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.component.WrittenBookContent;

/** One content source for the native field guide and serialized item metadata. */
public final class FieldGuide {
    private FieldGuide() {}

    public record Chapter(String title, String summary, String icon, int firstPage, int lastPage, boolean planned) {}
    public record Page(String title, List<String> paragraphs, int chapter) {}

    public static List<Chapter> chapters() {
        return List.of(
                new Chapter("Workshop", "Your first processing line", "coal_generator", 0, 4, false),
                new Chapter("Nexus", "Connect and use digital storage", "storage_terminal", 4, 6, false),
                new Chapter("Automation", "Move items between machines", "network_cable", 6, 7, false),
                new Chapter("Mining", "Set up a selective miner", "digital_miner", 7, 11, false),
                new Chapter("Controls", "Read the machine dashboard", "energy_cell", 11, 12, false),
                new Chapter("Diagnostics", "Get a stalled workshop running", "basic_circuit", 12, 14, false),
                new Chapter("Settings", "Tune your server", "advanced_circuit", 14, 15, false),
                new Chapter("Alpha status", "What is playable today", "field_guide", 15, 16, false),
                new Chapter("Future", "Ideas beyond this alpha", "chaotic_core", 16, 17, true));
    }

    /** Reflows the short archival lines into paragraphs for any native GUI size. */
    public static List<Page> pages() {
        List<Page> result = new ArrayList<>();
        List<Filterable<Component>> source = content().pages();
        List<Chapter> chapters = chapters();
        for (int i = 1; i < source.size(); i++) {
            String text = source.get(i).raw().getString();
            int titleEnd = text.indexOf("\n\n");
            String body = text.substring(titleEnd + 2, text.lastIndexOf("\n\nContents"));
            List<String> paragraphs = new ArrayList<>();
            // Terminal controls form a reference list; other articles use prose paragraphs.
            for (String paragraph : body.split(i == 6 ? "\n" : "\n\n")) {
                if (!paragraph.isBlank()) paragraphs.add(paragraph.replace('\n', ' '));
            }
            int chapter = 0;
            while (i - 1 >= chapters.get(chapter).lastPage()) chapter++;
            result.add(new Page(text.substring(0, titleEnd), List.copyOf(paragraphs), chapter));
        }
        return List.copyOf(result);
    }

    public static WrittenBookContent content() {
        List<Filterable<Component>> pages = new ArrayList<>();
        MutableComponent contents = Component.empty().append(heading("Technologia")).append("\n\n");
        contents.append(link("Getting started", 2)).append("\n");
        contents.append(link("Power", 3)).append("\n");
        contents.append(link("Processing", 4)).append("\n");
        contents.append(link("Storage", 6)).append("\n");
        contents.append(link("Automation", 8)).append("\n");
        contents.append(link("Selective miner", 9)).append("\n");
        contents.append(link("Machine controls", 13)).append("\n");
        contents.append(link("Troubleshooting", 14)).append("\n");
        contents.append(link("Server settings", 16)).append("\n");
        contents.append(link("What comes next", 17));
        pages.add(Filterable.passThrough(contents));

        pages.add(page("Your workshop",
                "Start with a coal",
                "generator, crusher",
                "and electric furnace.",
                "",
                "Craft this guide with",
                "a book and copper",
                "ingot, or use:",
                "/technologia guide"));
        pages.add(page("First power",
                "Place a generator",
                "beside a machine.",
                "Put coal or charcoal",
                "in its input slot.",
                "",
                "An energy cell stores",
                "surplus power and",
                "powers nearby blocks.",
                "Network cable is data."));
        pages.add(page("Crushing ores",
                "Feed raw iron, gold,",
                "copper, tin or lead",
                "into the input slot.",
                "",
                "One raw metal makes",
                "two dust. Move the",
                "dust to a powered",
                "electric furnace.",
                "Leave output space."));
        pages.add(page("Electric smelting",
                "A furnace uses power",
                "instead of fuel.",
                "Each dust smelts to",
                "an ingot. Normal",
                "smelting recipes work",
                "here too.",
                "",
                "The full loop gives",
                "two ingots per raw."));
        pages.add(page("Connect storage",
                "Place a storage core",
                "and a terminal. Join",
                "them with network",
                "cable, or touch them.",
                "",
                "The core holds items",
                "in 54 backing slots.",
                "The terminal is your",
                "view into the network."));
        pages.add(page("Using a terminal",
                "Search: name or ID.",
                "Sort: name or count.",
                "",
                "Left-click: a stack.",
                "Right-click: one item.",
                "Shift-click: transfer.",
                "Deposit: cursor stack.",
                "",
                "One core per terminal."));
        pages.add(page("Move items",
                "Use hoppers or item",
                "pipes to feed inputs",
                "and collect outputs.",
                "",
                "Output slots reject",
                "incoming items.",
                "Network cables carry",
                "storage connections,",
                "not machine power."));
        pages.add(page("Set up a miner",
                "The selective miner",
                "starts paused.",
                "Supply power, inspect",
                "the site, then enable",
                "it from its screen.",
                "",
                "It scans below itself:",
                "default 9 by 9 blocks,",
                "up to 32 blocks deep."));
        pages.add(page("Miner filters",
                "Put an ore block or",
                "its drop in the first",
                "slot to filter mining.",
                "The filter is kept.",
                "",
                "An empty filter allows",
                "all supported ores.",
                "It only mines ores an",
                "iron pick can harvest."));
        pages.add(page("Mine responsibly",
                "Owner must be online",
                "in this dimension.",
                "Claim support depends",
                "on your other mods.",
                "",
                "It skips block entities",
                "and does not load",
                "chunks for you.",
                "Leave output space."));
        pages.add(page("Miner stopped?",
                "Check power, outputs",
                "and loaded chunks.",
                "Only the owner can",
                "change its controls.",
                "",
                "After a scan finishes,",
                "use Rescan, then",
                "enable it to try again.",
                "Test your claim mod."));
        pages.add(page("Machine controls",
                "Read the status line",
                "before changes.",
                "Power and progress",
                "show what is missing.",
                "",
                "Pause stops work.",
                "Generators and cells",
                "can still share their",
                "stored power."));
        pages.add(page("No power?",
                "Check generator fuel",
                "and the pause state.",
                "Start with simple",
                "direct contact.",
                "",
                "A network cable is not",
                "an energy cable.",
                "External energy links",
                "need a compatible API."));
        pages.add(page("No processing?",
                "Check the input recipe",
                "and free output slots.",
                "Leave output space",
                "for the whole result.",
                "",
                "A storage terminal",
                "needs a loaded path",
                "to at least one core.",
                "No autocrafting yet."));
        pages.add(page("Server settings",
                "Balance settings live",
                "in the config folder:",
                "technologia.json",
                "",
                "The server owner can",
                "tune power costs and",
                "miner range there.",
                "Restart to apply them.",
                "Packs may tune these."));
        pages.add(page("This is an alpha",
                "Workshop machines,",
                "storage and the miner",
                "are playable systems.",
                "",
                "Art and balance will",
                "evolve with playtests.",
                "Draconic and chaotic",
                "cores are concepts.",
                "They have no powers."));
        pages.add(page("Future workshop",
                "Planned: autocrafting,",
                "chemical processing,",
                "reactors, resource",
                "farms, worlds, space",
                "and modular tools.",
                "",
                "The 18-stage roadmap",
                "is a proposal, not",
                "content already built."));

        return new WrittenBookContent(Filterable.passThrough("Technologia Field Guide"),
                "Technologia", 0, List.copyOf(pages), true);
    }

    private static Filterable<Component> page(String title, String... lines) {
        if (lines.length > 9) throw new IllegalArgumentException("Guide pages must stay readable");
        MutableComponent text = Component.empty().append(heading(title)).append("\n\n")
                .append(Component.literal(String.join("\n", lines)))
                .append("\n\n").append(link("Contents", 1));
        return Filterable.passThrough(text);
    }

    private static MutableComponent heading(String text) {
        return Component.literal(text).withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_AQUA);
    }

    private static MutableComponent link(String text, int page) {
        return Component.literal(text).withStyle(style -> style.withColor(ChatFormatting.DARK_BLUE)
                .withUnderlined(true).withClickEvent(new ClickEvent(ClickEvent.Action.CHANGE_PAGE,
                        Integer.toString(page))));
    }
}
