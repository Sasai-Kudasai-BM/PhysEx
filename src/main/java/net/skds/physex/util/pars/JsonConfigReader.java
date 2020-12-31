package net.skds.physex.util.pars;

import static net.skds.physex.PhysEX.LOGGER;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ITag;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.skds.physex.PhysEX;
import net.skds.physex.util.pars.ParsApplier.ParsGroup;

public class JsonConfigReader {

	File file;

	File dir = new File(System.getProperty("user.dir") + "\\config\\physex");
	Set<Map.Entry<String, JsonElement>> blockListSet = new HashSet<>();
	Set<Map.Entry<String, JsonElement>> propertyListSet = new HashSet<>();
	Map<String, JsonElement> propertyListMap = new HashMap<>();

	public Map<String, ParsGroup<FluidPars>> FP = new HashMap<>();
	public Map<String, ParsGroup<BlockPhysicsPars>> BFP = new HashMap<>();

	Gson GSON = new Gson();
	boolean created = false;

	public void run() {

		try {
			dir.mkdir();
			file = new File(dir, "fluid-config.json");
			boolean exsists = file.exists();

			if (!exsists) {
				create(false);
			}
			if (!readFluid(file) && !created) {
				create(true);
				readFluid(file);
			}
		} catch (IOException e) {
			LOGGER.error("Error while reading config: ", e);
		}
	}

	private void create(boolean existError) throws IOException {
		created = true;
		boolean copydeleted = true;
		File copyFile = new File(dir, "fluid-config-backup.json");
		if (existError && file.exists()) {
			LOGGER.warn("Config resers to default");
			if (copyFile.exists()) {
				copydeleted = copyFile.delete();
			}
			if (copydeleted) {
				Files.copy(file.toPath(), copyFile.toPath());
				LOGGER.warn("Config backup created");
			}
		}
		BufferedInputStream is = new BufferedInputStream(PhysEX.class.getClassLoader().getResourceAsStream("physex\\special\\defaults.json"));
		boolean ex = file.exists();
		if (ex) {
			file.delete();
			// LOGGER.info(file.delete());
		}
		Files.copy(is, file.toPath());
		is.close();
	}

	private boolean readFluid(File file) throws IOException {
		JsonObject jsonobject = new JsonObject();

		InputStream inputStream = new FileInputStream(file);
		Reader r = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
		JsonReader jsonReader = new JsonReader(r);
		try {
			jsonobject = GSON.getAdapter(JsonObject.class).read(jsonReader);
		} catch (IOException e) {
			LOGGER.error("Empty or invalid config file!");

			inputStream.close();
			create(true);
			inputStream = new FileInputStream(file);
			r = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
			jsonReader = new JsonReader(r);

			jsonobject = GSON.getAdapter(JsonObject.class).read(jsonReader);
		}
		r.close();
		jsonReader.close();

		JsonElement bls = jsonobject.get("BlockLists");
		JsonElement pls = jsonobject.get("PropertyLists");
		if (bls == null || pls == null) {
			LOGGER.error("Invalid config file!");
			return false;
		}
		blockListSet = bls.getAsJsonObject().entrySet();
		propertyListSet = pls.getAsJsonObject().entrySet();

		if (blockListSet.size() == 0) {
			LOGGER.error("Empty block list file!");
			return false;
		}

		propertyListSet.forEach(entry -> {
			propertyListMap.put(entry.getKey(), entry.getValue());
		});

		for (Map.Entry<String, JsonElement> entry : blockListSet) {
			ArrayList<String> blockIDs = new ArrayList<>();
			String key = entry.getKey();
			JsonElement listElement = entry.getValue();
			if (!listElement.isJsonArray()) {
				LOGGER.error("Block list \"" + key + "\" is not a list!");
				return false;
			}
			JsonArray blocklist = listElement.getAsJsonArray();
			if (blocklist.size() == 0) {
				LOGGER.warn("Block list \"" + key + "\" is empty!");
			}
			JsonElement properties = propertyListMap.get(key);
			if (properties == null) {
				LOGGER.error("Block list \"" + key + "\" have no properties!");
				return false;
			}
			blocklist.forEach(element -> {
				blockIDs.add(element.getAsString());
			});

			//LOGGER.info(key + blocklist.toString() + properties.toString());

			addFluidParsGroup(key, blocklist, FluidPars.readFromJson(properties, key));

		}
		return true;
	}

	private void addFluidParsGroup(String key, JsonArray blockNames, FluidPars pars) {
		if (pars == null) {
			return;
		}
		Set<Block> blocks = new HashSet<>();
		for (JsonElement je : blockNames) {
			String id = je.getAsString();
			if (id.charAt(0) == '#') {
				id = id.substring(1);
				ITag<Block> tag = BlockTags.getCollection().get(new ResourceLocation(id));
				if (tag == null) {					
					LOGGER.error("Block tag \"" + id + "\" does not exist!");
					continue;
				}
				blocks.addAll(tag.func_230236_b_());
				continue;
			}
			Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
			if (block != null && block != Blocks.AIR) {
				blocks.add(block);
			} else {
				LOGGER.error("Block \"" + id + "\" does not exist!");
			}
		}

		ParsGroup<FluidPars> group = new ParsGroup<FluidPars>(pars, blocks);
		FP.put(key, group);
	}
}