package net.mohron.skyclaims.command;

import com.flowpowered.math.vector.Vector3i;
import net.mohron.skyclaims.SchematicHandler;
import net.mohron.skyclaims.SkyClaims;
import net.mohron.skyclaims.lib.Permissions;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataTranslators;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.extent.ArchetypeVolume;
import org.spongepowered.api.world.schematic.BlockPaletteTypes;
import org.spongepowered.api.world.schematic.Schematic;

import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.GZIPOutputStream;

import static org.spongepowered.api.command.args.GenericArguments.seq;
import static org.spongepowered.api.command.args.GenericArguments.string;

public class CommandCreateSchematic implements CommandExecutor {
	private static final SkyClaims PLUGIN = SkyClaims.getInstance();
	private static final File CONFIG_DIR = new File(PLUGIN.getConfigDir().toString());

	public static String helpText = "used to save the selected area as an island schematic";

	public static CommandSpec commandSpec = CommandSpec.builder()
			.permission(Permissions.COMMAND_CREATE_SCHEMATIC)
			.description(Text.of(helpText))
			.arguments(seq(string(Text.of("format")), string(Text.of("name"))))
			.executor(new CommandCreateSchematic())
			.build();

	public static void register() {
		try {
			PLUGIN.getGame().getCommandManager().register(PLUGIN, commandSpec);
			PLUGIN.getLogger().info("Registered command: CommandCreateSchematic");
		} catch (UnsupportedOperationException e) {
			e.printStackTrace();
			PLUGIN.getLogger().error("Failed to register command: CommandCreateSchematic");
		}
	}

	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		if (!(src instanceof Player)) {
			src.sendMessage(Text.of(TextColors.RED, "You must be a player to use this command."));
			return CommandResult.success();
		}
		Player player = (Player) src;
		SchematicHandler.PlayerData data = SchematicHandler.get(player);
		if (data.getPos1() == null || data.getPos2() == null) {
			player.sendMessage(Text.of(TextColors.RED, "You must set both positions before copying"));
			return CommandResult.success();
		}
		Vector3i min = data.getPos1().min(data.getPos2());
		Vector3i max = data.getPos1().max(data.getPos2());
		ArchetypeVolume volume = player.getWorld().createArchetypeVolume(min, max, player.getLocation().getPosition().toInt());

		String format = args.getOne("format").get().toString();
		String name = args.getOne("name").get().toString();

		if (!"legacy".equalsIgnoreCase(format) && !"sponge".equalsIgnoreCase(format)) {
			player.sendMessage(Text.of(TextColors.RED, "Unsupported schematic format, supported formats are [legacy, sponge]"));
			return CommandResult.success();
		}
		Schematic schematic = Schematic.builder()
				.volume(volume)
				.metaValue(Schematic.METADATA_AUTHOR, player.getName())
				.metaValue(Schematic.METADATA_NAME, name)
				.paletteType(BlockPaletteTypes.LOCAL)
				.build();
		DataContainer schematicData = null;
		if ("legacy".equalsIgnoreCase(format)) {
			schematicData = DataTranslators.LEGACY_SCHEMATIC.translate(schematic);
		} else if ("sponge".equalsIgnoreCase(format)) {
			schematicData = DataTranslators.SCHEMATIC.translate(schematic);
		}
		File outputFile = new File(CONFIG_DIR, name + ".schematic");
		try {
			DataFormats.NBT.writeTo(new GZIPOutputStream(new FileOutputStream(outputFile)), schematicData);
			player.sendMessage(Text.of(TextColors.GREEN, "Saved schematic to " + outputFile.getAbsolutePath()));
		} catch (Exception e) {
			e.printStackTrace();
			player.sendMessage(Text.of(TextColors.DARK_RED, "Error saving schematic: " + e.getMessage()));
			return CommandResult.success();
		}
		return CommandResult.success();
	}
}