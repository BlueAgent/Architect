package li.cil.architect.common.item;

import li.cil.architect.common.config.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public abstract class AbstractProvider extends AbstractItem {
    // --------------------------------------------------------------------- //
    // Computed data.

    // NBT tag names.
    private static final String TAG_DIMENSION = "dimension";
    private static final String TAG_POSITION = "position";
    private static final String TAG_SIDE = "side";

    // --------------------------------------------------------------------- //

    AbstractProvider() {
        setMaxStackSize(1);
    }

    /**
     * Check whether this provider is currently bound to a position.
     *
     * @param stack the provider to check for.
     * @return <code>true</code> if the provider is bound; <code>false</code> otherwise.
     */
    public static boolean isBound(final ItemStack stack) {
        final NBTTagCompound dataNbt = getDataTag(stack);
        return dataNbt.hasKey(TAG_DIMENSION, NBT.TAG_INT) &&
               dataNbt.hasKey(TAG_POSITION, NBT.TAG_LONG) &&
               dataNbt.hasKey(TAG_SIDE, NBT.TAG_BYTE);
    }

    /**
     * Get the dimension the position the provider is bound to is in.
     * <p>
     * Behavior is undefined if {@link #isBound(ItemStack)} returns <code>false</code>.
     *
     * @param stack the provider to get the dimension for.
     * @return the dimension of the position the provider is bound to.
     */
    public static int getDimension(final ItemStack stack) {
        final NBTTagCompound dataNbt = getDataTag(stack);
        return dataNbt.getInteger(TAG_DIMENSION);
    }

    /**
     * Get the position the provider is bound to.
     * <p>
     * Behavior is undefined if {@link #isBound(ItemStack)} returns <code>false</code>.
     *
     * @param stack the provider to get the position for.
     * @return the position the provider is bound to.
     */
    public static BlockPos getPosition(final ItemStack stack) {
        final NBTTagCompound dataNbt = getDataTag(stack);
        return BlockPos.fromLong(dataNbt.getLong(TAG_POSITION));
    }

    /**
     * Get the side of the block the provider is bound to.
     * <p>
     * Behavior is undefined if {@link #isBound(ItemStack)} returns <code>false</code>.
     *
     * @param stack the provider to get the bound-to side for.
     * @return the side the provider is bound to.
     */
    public static EnumFacing getSide(final ItemStack stack) {
        final NBTTagCompound dataNbt = getDataTag(stack);
        return EnumFacing.VALUES[dataNbt.getByte(TAG_SIDE) & 0xFF];
    }

    // --------------------------------------------------------------------- //
    // Item

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(final ItemStack stack, final EntityPlayer player, final List<String> tooltip, final boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        final String info = I18n.format(getTooltip());
        final FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
        tooltip.addAll(fontRenderer.listFormattedStringToWidth(info, Constants.MAX_TOOLTIP_WIDTH));

        if (isBound(stack)) {
            final BlockPos pos = getPosition(stack);
            tooltip.add(I18n.format(Constants.TOOLTIP_PROVIDER_TARGET, pos.getX(), pos.getY(), pos.getZ()));
        }
    }

    @Override
    public EnumActionResult onItemUse(final EntityPlayer player, final World world, final BlockPos pos, final EnumHand hand, final EnumFacing side, final float hitX, final float hitY, final float hitZ) {
        final TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity != null && isValidTarget(tileEntity, side)) {
            if (player.isSneaking() && !world.isRemote) {
                final ItemStack stack = player.getHeldItem(hand);
                final NBTTagCompound dataNbt = getDataTag(stack);
                dataNbt.setInteger(TAG_DIMENSION, world.provider.getDimension());
                dataNbt.setLong(TAG_POSITION, pos.toLong());
                dataNbt.setByte(TAG_SIDE, (byte) side.getIndex());
                player.inventory.markDirty();
            }
            return EnumActionResult.SUCCESS;
        }
        return EnumActionResult.PASS;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(final World world, final EntityPlayer player, final EnumHand hand) {
        final ItemStack stack = player.getHeldItem(hand);
        if (player.isSneaking()) {
            if (!world.isRemote) {
                final NBTTagCompound dataNbt = getDataTag(stack);
                dataNbt.removeTag(TAG_DIMENSION);
                dataNbt.removeTag(TAG_POSITION);
                dataNbt.removeTag(TAG_SIDE);
                player.inventory.markDirty();
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        return new ActionResult<>(EnumActionResult.PASS, stack);
    }

    abstract protected boolean isValidTarget(final TileEntity tileEntity, final EnumFacing side);

    abstract protected String getTooltip();
}
