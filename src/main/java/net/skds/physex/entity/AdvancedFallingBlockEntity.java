package net.skds.physex.entity;

import java.util.List;
import java.util.stream.Stream;

import com.google.common.collect.Lists;

import net.minecraft.block.AnvilBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.FallingBlockEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.tags.BlockTags;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ReuseableStream;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.NetworkHooks;
import net.skds.physex.blockphysics.BFTask;
import net.skds.physex.registry.Entities;
import net.skds.physex.util.Interface.IBlockExtended;
import net.skds.physex.util.pars.BlockPhysicsPars;

public class AdvancedFallingBlockEntity extends Entity implements IEntityAdditionalSpawnData {

	private final float G = 9.81F;
	private final float TG = G / 400;
	private final float BDZ = TG + 0.05F;

	public float bounce = 0.5F;
	public float mass = 1000;
	public float strength = 10.0F;

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

	@Override
	protected boolean canTriggerWalking() {
		return false;
	}

	@Override
	protected void registerData() {
		this.dataManager.register(ORIGIN, BlockPos.ZERO);
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean canBeCollidedWith() {
		return !this.removed;
	}

	public void tick() {

		// if (world.isRemote) {
		// Vector3d m = getMotion();
		// m = getMotion();
		// this.setPosition(getPosX() + m.getX(), getPosY() + m.getY(), getPosZ() +
		// m.getZ());
		// return;
		// }
		++fallTime;

		moveCustom();
		if (!this.world.isRemote) {
			BlockPos blockpos1 = this.func_233580_cy_();

			if (!this.onGround) {
				if (!this.world.isRemote && (this.fallTime > 100 && (blockpos1.getY() < 1 || blockpos1.getY() > 256)
						|| this.fallTime > 600)) {

					this.remove();
				}
			} else {
				BlockState blockstate = this.world.getBlockState(blockpos1);
				if (!blockstate.isIn(Blocks.MOVING_PISTON)) {

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

	private void moveCustom() {
		Vector3d motion = getMotion();

		if (!this.hasNoGravity()) {
			motion = motion.add(0.0D, -TG, 0.0D);
		}
		Vector3d maxMove = getAllowedMovement(motion);

		if (!maxMove.equals(motion)) {
			onCollision(motion, maxMove);
			motion = getMotion();
		} else {
			setMotion(motion);
		}

		this.setPosition(getPosX() + motion.getX(), getPosY() + motion.getY(), getPosZ() + motion.getZ());
	}

	private void onCollision(Vector3d motion, Vector3d maxMove) {
		boolean collideX = motion.x != maxMove.x;
		boolean collideY = motion.y != maxMove.y;
		boolean collideZ = motion.z != maxMove.z;
		this.collidedVertically = collideY;

		double vx = collideX ? (-inThreadhold(motion.x) * bounce) : motion.x;
		double vy = collideY ? (-inThreadhold(motion.y) * bounce) : motion.y;
		double vz = collideZ ? (-inThreadhold(motion.z) * bounce) : motion.z;

		this.onGround = this.collidedVertically && Math.abs(vy) < BDZ;
		setMotion(vx, vy, vz);
	}

	private double inThreadhold(double d) {
		return Math.abs(d) > BDZ ? d : 0;
	}

	private Vector3d getAllowedMovement(Vector3d vec) {
		AxisAlignedBB axisalignedbb = this.getBoundingBox();
		ISelectionContext iselectioncontext = ISelectionContext.forEntity(this);
		VoxelShape voxelshape = this.world.getWorldBorder().getShape();
		Stream<VoxelShape> stream = VoxelShapes.compare(voxelshape, VoxelShapes.create(axisalignedbb.shrink(1.0E-7D)),
				IBooleanFunction.AND) ? Stream.empty() : Stream.of(voxelshape);
		// Stream<VoxelShape> stream1 = this.world.func_230318_c_(this,
		// axisalignedbb.expand(vec), (p_233561_0_) -> {
		// return true;
		// });
		// ReuseableStream<VoxelShape> reuseablestream = new
		// ReuseableStream<>(Stream.concat(stream1, stream));
		ReuseableStream<VoxelShape> reuseablestream = new ReuseableStream<>(stream);
		Vector3d vector3d = vec.lengthSquared() == 0.0D ? vec
				: collideBoundingBoxHeuristically(this, vec, axisalignedbb, this.world, iselectioncontext,
						reuseablestream);
		boolean flag = vec.x != vector3d.x;
		boolean flag1 = vec.y != vector3d.y;
		boolean flag2 = vec.z != vector3d.z;
		boolean flag3 = this.onGround || flag1 && vec.y < 0.0D;
		if (this.stepHeight > 0.0F && flag3 && (flag || flag2)) {
			Vector3d vector3d1 = collideBoundingBoxHeuristically(this,
					new Vector3d(vec.x, (double) this.stepHeight, vec.z), axisalignedbb, this.world, iselectioncontext,
					reuseablestream);
			Vector3d vector3d2 = collideBoundingBoxHeuristically(this,
					new Vector3d(0.0D, (double) this.stepHeight, 0.0D), axisalignedbb.expand(vec.x, 0.0D, vec.z),
					this.world, iselectioncontext, reuseablestream);
			if (vector3d2.y < (double) this.stepHeight) {
				Vector3d vector3d3 = collideBoundingBoxHeuristically(this, new Vector3d(vec.x, 0.0D, vec.z),
						axisalignedbb.offset(vector3d2), this.world, iselectioncontext, reuseablestream).add(vector3d2);
				if (horizontalMag(vector3d3) > horizontalMag(vector3d1)) {
					vector3d1 = vector3d3;
				}
			}

			if (horizontalMag(vector3d1) > horizontalMag(vector3d)) {
				return vector3d1
						.add(collideBoundingBoxHeuristically(this, new Vector3d(0.0D, -vector3d1.y + vec.y, 0.0D),
								axisalignedbb.offset(vector3d1), this.world, iselectioncontext, reuseablestream));
			}
		}

		return vector3d;
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

		BlockState ns = NBTUtil.readBlockState(compound.getCompound("BlockState"));
		if (ns != this.fallTile) {
			this.fallTile = ns;
			BlockPhysicsPars pars = getParam(fallTile);

			this.bounce = pars.bounce;
			this.mass = pars.mass;
			this.strength = pars.strength;
		}

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

	private BlockPhysicsPars getParam(Block b) {
		BlockPhysicsPars par = ((IBlockExtended) b).getCustomBlockPars().getBlockPhysicsPars();
		if (par == null) {
			boolean empty = b.getDefaultState()
					.getCollisionShape(world, new BlockPos(this.getPosX(), this.getPosY(), this.getPosZ())).isEmpty();
			@SuppressWarnings("deprecation")
			float res = b.getExplosionResistance();
			par = new BlockPhysicsPars(b, empty, res);
		}
		return par;
	}

	private BlockPhysicsPars getParam(BlockState s) {
		return getParam(s.getBlock());
	}

}