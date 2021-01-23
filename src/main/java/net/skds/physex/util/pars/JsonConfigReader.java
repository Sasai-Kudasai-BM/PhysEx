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

	File fileF;
	File fileB;

	File dir = new File(System.getProperty("user.dir") + "\\config\\physex");
	Set<Map.Entry<String, JsonElement>> blockListSet = new HashSet<>();
	Set<Map.Entry<String, JsonElement>> propertyListSet = new HashSet<>();
	Map<String, JsonElement> propertyListMap = new HashMap<>();

	public Map<String, ParsGroup<FluidPars>> FP = new HashMap<>();
	public Map<String, ParsGroup<BlockPhysicsPars>> BFP = new HashMap<>();

	Gson GSON = new Gson();
	boolean created = false;

	public void run() {

		dir.mkdir();

		try {
			fileF = new File(dir, "fluid-config.json");
			boolean exsists = fileF.exists();

			if (!exsists) {
				create(false);
			}
			if (!readFluid(fileF) && !created) {
				create(true);
				readFluid(fileF);
			}
		} catch (IOException e) {
			LOGGER.error("Error while reading config: ", e);
		}

		try {
			fileB = new File(dir, "blockphysics.json");
			boolean exsists = fileB.exists();

			if (!exsists) {
				createB(false);
			}
			if (!readBlocks(fileB) && !created) {
				createB(true);
				readBlocks(fileB);
			}
		} catch (IOException e) {
			LOGGER.error("Error while reading config: ", e);
		}
	}

	private void createB(boolean existError) throws IOException {
		created = true;
		boolean copydeleted = true;
		File copyFile = new File(dir, "blockphysics-backup.json");
		if (existError && fileB.exists()) {
			LOGGER.warn("Blockphysics config resers to default");
			if (copyFile.exists()) {
				copydeleted = copyFile.delete();
			}
			if (copydeleted) {
				Files.copy(fileB.toPath(), copyFile.toPath());
				LOGGER.warn("Blockphysics config backup created");
			}
		}
		BufferedInputStream is = new BufferedInputStream(
				PhysEX.class.getClassLoader().getResourceAsStream("physex\\special\\blocks.json"));
		boolean ex = fileB.exists();
		if (ex) {
			fileB.delete();
			// LOGGER.info(fileF.delete());
		}
		Files.copy(is, fileB.toPath());
		is.close();
	}

	private void create(boolean existError) throws IOException {
		created = true;
		boolean copydeleted = true;
		File copyFile = new File(dir, "fluid-config-backup.json");
		if (existError && fileF.exists()) {
			LOGGER.warn("Fluid config resers to default");
			if (copyFile.exists()) {
				copydeleted = copyFile.delete();
			}
			if (copydeleted) {
				Files.copy(fileF.toPath(), copyFile.toPath());
				LOGGER.warn("Fluid config backup created");
			}
		}
		BufferedInputStream is = new BufferedInputStream(
				PhysEX.class.getClassLoader().getResourceAsStream("physex\\special\\fluids.json"));
		boolean ex = fileF.exists();
		if (ex) {
			fileF.delete();
			// LOGGER.info(fileF.delete());
		}
		Files.copy(is, fileF.toPath());
		is.close();
	}

	private boolean readBlocks(File file) throws IOException {
		JsonObject jsonobject = new JsonObject();

		InputStream inputStream = new FileInputStream(file);
		Reader r = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
		JsonReader jsonReader = new JsonReader(r);
		try {
			jsonobject = GSON.getAdapter(JsonObject.class).read(jsonReader);
		} catch (IOException e) {
			LOGGER.error("Empty or invalid blockphysics config file!");

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
		JsonElement pls = jsonobject.get("Properties");
		if (bls == null || pls == null) {
			LOGGER.error("Invalid blockphysics config file!");
			return false;
		}
		Set<Map.Entry<String, JsonElement>> blockListSet = bls.getAsJsonObject().entrySet();
		Set<Map.Entry<String, JsonElement>> propertyListSet = pls.getAsJsonObject().entrySet();

		createBlock2PropMap(blockListSet, propertyListSet);

		return true;
	}

	private boolean readFluid(File fileF) throws IOException {
		JsonObject jsonobject = new JsonObject();

		InputStream inputStream = new FileInputStream(fileF);
		Reader r = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
		JsonReader jsonReader = new JsonReader(r);
		try {
			jsonobject = GSON.getAdapter(JsonObject.class).read(jsonReader);
		} catch (IOException e) {
			LOGGER.error("Empty or invalid fluid config file!");

			inputStream.close();
			create(true);
			inputStream = new FileInputStream(fileF);
			r = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
			jsonReader = new JsonReader(r);

			jsonobject = GSON.getAdapter(JsonObject.class).read(jsonReader);
		}
		r.close();
		jsonReader.close();

		JsonElement bls = jsonobject.get("BlockLists");
		JsonElement pls = jsonobject.get("PropertyLists");
		if (bls == null || pls == null) {
			LOGGER.error("Invalid fluid config file!");
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

			// LOGGER.info(key + blocklist.toString() + properties.toString());

			addFluidParsGroup(key, blocklist, FluidPars.readFromJson(properties, key));

		}
		return true;
	}

	private void createBlock2PropMap(Set<Map.Entry<String, JsonElement>> blocks, Set<Map.Entry<String, JsonElement>> props) {

		Map<String, Set<Block>> blocklists = new HashMap<>();
		for (Map.Entry<String, JsonElement> e : blocks) {
			String key = e.getKey();
			try {				
				JsonArray ja = e.getValue().getAsJsonArray();
				blocklists.put(key, getBlocksFromJA(ja));
			} catch (Exception ex) {
				LOGGER.error("Block list \"" + key + "\" is invalid!");
			}
		}

		for (Map.Entry<String, JsonElement> e : props) {

			String key = e.getKey();
			ParsGroup<BlockPhysicsPars> group = BlockPhysicsPars.readFromJson(e.getValue(), e.getKey(), blocklists);;
			if (group != null) {
				BFP.put(key, group);
			}
		}
	}

	private void addFluidParsGroup(String key, JsonArray blockNames, FluidPars pars) {
		if (pars == null) {
			return;
		}
		Set<Block> blocks = getBlocksFromJA(blockNames);

		ParsGroup<FluidPars> group = new ParsGroup<FluidPars>(pars, blocks);
		FP.put(key, group);
	}

	public static Set<Block> getBlocksFromString(Set<String> list) {
		Set<Block> blocks = new HashSet<>();
		for (String id : list) {
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
		return blocks;
	}

	public static Set<Block> getBlocksFromJA(JsonArray arr) {
		Set<Block> blocks = new HashSet<>();
		for (JsonElement je : arr) {
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
		return blocks;
	}
}