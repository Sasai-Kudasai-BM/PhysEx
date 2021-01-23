package net.skds.physex.util.pars;

import static net.skds.physex.PhysEX.LOGGER;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.skds.physex.util.pars.ParsApplier.ParsGroup;

public class BlockPhysicsPars {

	public final float mass, strength, bounce;
	public final int radial, linear, arc;
	public final boolean slide, hanging, attach, falling, celling, fragile;
	public final Set<Block> attachIgnore, selfList;
	public final String name;

	public BlockPhysicsPars(float mass, float strength, float bounce, int radial, int linear, int arc, boolean slide,
			boolean hanging, boolean attach, Boolean falling, Boolean celling, Boolean fragile, Set<Block> attachIgnore,
			Set<Block> selfList, String name) {
		this.mass = mass;
		this.strength = strength;
		this.bounce = bounce;
		this.radial = radial;
		this.linear = linear;
		this.arc = arc;
		this.falling = falling;
		this.celling = celling;
		this.fragile = fragile;
		this.slide = slide;
		this.hanging = hanging;
		this.attach = attach;
		this.attachIgnore = attachIgnore;
		this.selfList = selfList;
		this.name = name;
	}

	public BlockPhysicsPars(Block b, boolean empty, float resistance) {

		this.name = "Undefined";

		if (b instanceof AirBlock) {

			this.mass = 0;
			this.strength = 0;
			this.bounce = 0;
			this.radial = 0;
			this.linear = 0;
			this.arc = 0;
			this.falling = false;
			this.celling = false;
			this.fragile = true;
			this.slide = false;
			this.hanging = false;
			this.attach = false;
			this.attachIgnore = new HashSet<>();
			this.selfList = new HashSet<>();
			return;
		}

		float str = resistance;
		boolean bool = str > 3_000_000;
		boolean bool2 = str < 0.01F || empty;

		if (bool2 || bool) {
			str = 0.01F;
			if (bool) {
				str = 10.0F;
				this.fragile = false;
			} else {
				this.fragile = true;
			}
			this.falling = false;
		} else {
			this.falling = true;
			this.fragile = false;
		}
		this.mass = 1000;
		this.strength = (float) (str * 10);
		this.bounce = 0F;
		this.radial = 3;
		this.linear = 3;
		this.arc = (int) (str / 4);
		this.slide = str < 1;
		this.celling = false;
		this.hanging = false;
		this.attach = true;
		this.attachIgnore = new HashSet<>();
		this.selfList = new HashSet<>();

		// if (b == Blocks.BEDROCK) {
		// System.out.println(this);
		// }
	}

	public static ParsGroup<BlockPhysicsPars> readFromJson(JsonElement json, String name,
			Map<String, Set<Block>> blMap) {
		if (json == null) {
			LOGGER.error("Invalid blockphysics properties: \"" + name + "\"");
			return null;
		}
		JsonObject jsonObject = json.getAsJsonObject();
		Set<Block> blocks = new HashSet<>();

		JsonElement listsE = jsonObject.get("blocks");

		JsonElement Jmass = jsonObject.get("mass");
		JsonElement Jstrength = jsonObject.get("strength");
		JsonElement Jbounce = jsonObject.get("bounce");
		JsonElement Jradial = jsonObject.get("radial");
		JsonElement Jlinear = jsonObject.get("linear");
		JsonElement Jcelling = jsonObject.get("celling");
		JsonElement Jarc = jsonObject.get("arc");
		JsonElement Jslide = jsonObject.get("slide");
		JsonElement Jfalling = jsonObject.get("falling");
		JsonElement Jfragile = jsonObject.get("fragile");
		JsonElement Jhanging = jsonObject.get("hanging");
		JsonElement Jattach = jsonObject.get("attach");
		JsonElement JattachIgnore = jsonObject.get("attachIgnore");
		try {
			JsonArray listarr = listsE.getAsJsonArray();
			listarr.forEach((e) -> {
				String key = e.getAsString();
				Set<Block> set = blMap.get(key);
				if (set != null) {
					blocks.addAll(set);
				}
			});

			float mass = Jmass.getAsFloat();
			float strength = Jstrength.getAsFloat();
			float bounce = Jbounce.getAsFloat();
			int radial = Jradial.getAsInt();
			int linear = Jlinear.getAsInt();
			int arc = Jarc.getAsInt();
			boolean falling = Jfalling.getAsBoolean();
			boolean celling = Jcelling.getAsBoolean();
			boolean fragile = Jfragile.getAsBoolean();
			boolean slide = Jslide.getAsBoolean();
			boolean hanging = Jhanging.getAsBoolean();
			boolean attach = Jattach.getAsBoolean();

			Set<Block> attachIgnore = new HashSet<>();
			JattachIgnore.getAsJsonArray().forEach((e) -> {
				String key = e.getAsString();
				Set<Block> set = blMap.get(key);
				if (set != null) {
					attachIgnore.addAll(set);
				}
			});

			BlockPhysicsPars pars = new BlockPhysicsPars(mass, strength, bounce, radial, linear, arc, slide, hanging,
					attach, falling, celling, fragile, attachIgnore, blocks, name);

			// System.out.println(pars);
			// System.out.println(blocks);

			return new ParsGroup<BlockPhysicsPars>(pars, blocks);

		} catch (Exception e) {
			LOGGER.error("Invalid blockphysics properties: \"" + name + "\"", e);
			return null;
		}
	}

	@Override
	public String toString() {
		String ss = "\n========================\n";
		return ss + name + ":\n    mass: " + mass + "\n    strength: " + strength + "\n    radial: " + radial
				+ "\n    linear: " + linear + "\n    arc: " + arc + "\n    falling: " + falling + "\n    celling: "
				+ celling + "\n    falling: " + falling + "\n    slide: " + slide + "\n    hanging: " + hanging
				+ "\n    attach: " + attach + ss;
	}
}