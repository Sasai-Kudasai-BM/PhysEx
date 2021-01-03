package net.skds.physex.entity;

import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.block.AnvilBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ConcretePowderBlock;
import net.minecraft.block.FallingBlock;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.FallingBlockEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.DirectionalPlaceContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.NetworkHooks;
import net.skds.physex.registry.Entities;

public class AdvancedFallingBlockEntity extends Entity implements IEntityAdditionalSpawnData {
	private BlockState fallTile = Blocks.SAND.getDefaultState();
	public int fallTime;
	public boolean shouldDropItem = true;
	private boolean dontSetBlock;
	private boolean hurtEntities;
	private int fallHurtMax = 40;
	private float fallHurtAmount = 2.0F;
	public CompoundNBT tileEntityData;
	protected static final DataParameter<BlockPos> ORIGIN = EntityDataManager.createKey(FallingBlockEntity.class,
			DataSerializers.BLOCK_POS);

	public AdvancedFallingBlockEntity(EntityType<? extends Entity> t, World w) {
		super(t, w);
	}

	public static EntityType<? extends Entity> getForReg(String id) {
		EntityType<? extends Entity> type = EntityType.Builder
				.create(AdvancedFallingBlockEntity::new, EntityClassification.MISC).size(1.0F, 1.0F)
				.setTrackingRange(32).setUpdateInterval(5).immuneToFire().setShouldReceiveVelocityUpdates(true)
				.build(id);
		return type;
	}

	public AdvancedFallingBlockEntity(World worldIn, double x, double y, double z, BlockState fallingBlockState) {
		super(Entities.ADVANCED_FALLING_BLOCK.get(), worldIn);
		this.fallTile = fallingBlockState;
		this.preventEntitySpawning = true;
		this.setPosition(x, y + (double) ((1.0F - this.getHeight()) / 2.0F), z);
		this.setMotion(Vector3d.ZERO);
		this.prevPosX = x;
		this.prevPosY = y;
		this.prevPosZ = z;
		this.setOrigin(this.func_233580_cy_());
	}

	/**
	 * Returns true if it's possible to attack this entity with an item.
	 */
	public boolean canBeAttackedWithItem() {
		return false;
	}

	@Override
	public boolean func_241845_aY() {
		return true;
	}

	public void setOrigin(BlockPos p_184530_1_) {
		this.dataManager.set(ORIGIN, p_184530_1_);
	}

	@OnlyIn(Dist.CLIENT)
	public BlockPos getOrigin() {
		return this.dataManager.get(ORIGIN);
	}

	protected boolean canTriggerWalking() {
		return false;
	}

	protected void registerData() {
		this.dataManager.register(ORIGIN, BlockPos.ZERO);
	}

	@SuppressWarnings("deprecation")
	public boolean canBeCollidedWith() {
		return !this.removed;
	}

	@SuppressWarnings("deprecation")
	public void tick() {
		++fallTime;
		// System.out.println("x");
		if (this.fallTile.isAir()) {
			this.remove();
		} else {
			//Block block = this.fallTile.getBlock();
			// System.out.println("blob");

			if (!this.hasNoGravity()) {
				this.setMotion(this.getMotion().add(0.0D, -0.04D, 0.0D));
			}

			this.move(MoverType.SELF, this.getMotion());
			if (!this.world.isRemote) {
				BlockPos blockpos1 = this.func_233580_cy_();
				boolean flag = this.fallTile.getBlock() instanceof ConcretePowderBlock;
				boolean flag1 = flag && this.world.getFluidState(blockpos1).isTagged(FluidTags.WATER);
				double d0 = this.getMotion().lengthSquared();
				if (flag && d0 > 1.0D) {
					BlockRayTraceResult blockraytraceresult = this.world.rayTraceBlocks(new RayTraceContext(
							new Vector3d(this.prevPosX, this.prevPosY, this.prevPosZ), this.getPositionVec(),
							RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.SOURCE_ONLY, this));
					if (blockraytraceresult.getType() != RayTraceResult.Type.MISS
							&& this.world.getFluidState(blockraytraceresult.getPos()).isTagged(FluidTags.WATER)) {
						blockpos1 = blockraytraceresult.getPos();
						flag1 = true;
					}
				}

				if (!this.onGround && !flag1) {
					if (!this.world.isRemote && (this.fallTime > 100 && (blockpos1.getY() < 1 || blockpos1.getY() > 256)
							|| this.fallTime > 600)) {

						this.remove();
					}
				} else {
					BlockState blockstate = this.world.getBlockState(blockpos1);
					this.setMotion(this.getMotion().mul(0.7D, -0.5D, 0.7D));
					if (!blockstate.isIn(Blocks.MOVING_PISTON)) {
						if (!this.dontSetBlock) {
							boolean flag2 = blockstate.isReplaceable(new DirectionalPlaceContext(this.world, blockpos1,
									Direction.DOWN, ItemStack.EMPTY, Direction.UP));
							boolean flag3 = FallingBlock.canFallThrough(this.world.getBlockState(blockpos1.down()))
									&& (!flag || !flag1);
							boolean flag4 = this.fallTile.isValidPosition(this.world, blockpos1) && !flag3;
							if (flag2 && flag4) {
								if (this.fallTile.func_235901_b_(BlockStateProperties.WATERLOGGED)
										&& this.world.getFluidState(blockpos1).getFluid() == Fluids.WATER) {
									this.fallTile = this.fallTile.with(BlockStateProperties.WATERLOGGED,
											Boolean.valueOf(true));
								}

								if (this.world.setBlockState(blockpos1, this.fallTile, 3)) {

									this.remove();

									if (this.tileEntityData != null && this.fallTile.hasTileEntity()) {
										TileEntity tileentity = this.world.getTileEntity(blockpos1);
										if (tileentity != null) {
											CompoundNBT compoundnbt = tileentity.write(new CompoundNBT());

											for (String s : this.tileEntityData.keySet()) {
												INBT inbt = this.tileEntityData.get(s);
												if (!"x".equals(s) && !"y".equals(s) && !"z".equals(s)) {
													compoundnbt.put(s, inbt.copy());
												}
											}

											tileentity.func_230337_a_(this.fallTile, compoundnbt);
											tileentity.markDirty();
										}
									}
								}
							}
						}
					}
				}
			}

			this.setMotion(this.getMotion().scale(0.98D));
		}
	}

	public boolean onLivingFall(float distance, float damageMultiplier) {
		if (this.hurtEntities) {
			int i = MathHelper.ceil(distance - 1.0F);
			if (i > 0) {
				List<Entity> list = Lists
						.newArrayList(this.world.getEntitiesWithinAABBExcludingEntity(this, this.getBoundingBox()));
				boolean flag = this.fallTile.func_235714_a_(BlockTags.ANVIL);
				DamageSource damagesource = flag ? DamageSource.ANVIL : DamageSource.FALLING_BLOCK;

				for (Entity entity : list) {
					entity.attackEntityFrom(damagesource,
							(float) Math.min(MathHelper.floor((float) i * this.fallHurtAmount), this.fallHurtMax));
				}

				if (flag && (double) this.rand.nextFloat() < (double) 0.05F + (double) i * 0.05D) {
					BlockState blockstate = AnvilBlock.damage(this.fallTile);
					if (blockstate == null) {
						this.dontSetBlock = true;
					} else {
						this.fallTile = blockstate;
					}
				}
			}
		}

		return false;
	}

	protected void writeAdditional(CompoundNBT compound) {
		compound.put("BlockState", NBTUtil.writeBlockState(this.fallTile));
		compound.putInt("Time", this.fallTime);
		compound.putBoolean("DropItem", this.shouldDropItem);
		compound.putBoolean("HurtEntities", this.hurtEntities);
		compound.putFloat("FallHurtAmount", this.fallHurtAmount);
		compound.putInt("FallHurtMax", this.fallHurtMax);
		if (this.tileEntityData != null) {
			compound.put("TileEntityData", this.tileEntityData);
		}

	}

	@SuppressWarnings("deprecation")
	protected void readAdditional(CompoundNBT compound) {
		this.fallTile = NBTUtil.readBlockState(compound.getCompound("BlockState"));
		this.fallTime = compound.getInt("Time");
		if (compound.contains("HurtEntities", 99)) {
			this.hurtEntities = compound.getBoolean("HurtEntities");
			this.fallHurtAmount = compound.getFloat("FallHurtAmount");
			this.fallHurtMax = compound.getInt("FallHurtMax");
		} else if (this.fallTile.func_235714_a_(BlockTags.ANVIL)) {
			this.hurtEntities = true;
		}

		if (compound.contains("DropItem", 99)) {
			this.shouldDropItem = compound.getBoolean("DropItem");
		}

		if (compound.contains("TileEntityData", 10)) {
			this.tileEntityData = compound.getCompound("TileEntityData");
		}

		if (this.fallTile.isAir()) {
			this.fallTile = Blocks.SAND.getDefaultState();
		}

	}

	@OnlyIn(Dist.CLIENT)
	public World getWorldObj() {
		return this.world;
	}

	public void setHurtEntities(boolean hurtEntitiesIn) {
		this.hurtEntities = hurtEntitiesIn;
	}

	@OnlyIn(Dist.CLIENT)
	public boolean canRenderOnFire() {
		return false;
	}

	public void fillCrashReport(CrashReportCategory category) {
		super.fillCrashReport(category);
		category.addDetail("Immitating BlockState", this.fallTile.toString());
	}

	public BlockState getBlockState() {
		return this.fallTile;
	}

	public boolean ignoreItemEntityData() {
		return true;
	}

	public IPacket<?> createSpawnPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	@Override
	public void writeSpawnData(PacketBuffer buffer) {
		buffer.writeCompoundTag(NBTUtil.writeBlockState(fallTile));
	}

	@Override
	public void readSpawnData(PacketBuffer additionalData) {
		fallTile = NBTUtil.readBlockState(additionalData.readCompoundTag());
	}
}