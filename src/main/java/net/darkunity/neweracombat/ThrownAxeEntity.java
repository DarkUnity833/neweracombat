package net.darkunity.neweracombat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

import java.util.Optional;

/**
 * ThrownAxeEntity - Ультимативная, расширенная реализация метательного топора.
 * Вес: Тяжелый (баллистика: 16 блоков ровно, затем падение).
 * Эффекты: Искры, Хедшоты, Громовержец, Верность.
 * Исправлен баг исчезновения при возврате во время кулдауна.
 * ФИКС: Защита от уничтожения молнией и огнем.
 * ДОБАВЛЕНО: Снятие стамины 5.0 при броске.
 */
public class ThrownAxeEntity extends ThrowableItemProjectile {
    
    // Синхронизатор для связи сервера с рендерером (клиентом)
    private static final EntityDataAccessor<Boolean> DATA_STUCK = SynchedEntityData.defineId(ThrownAxeEntity.class, EntityDataSerializers.BOOLEAN);

    private float baseDamage = 0;
    private int flightTicks = 0;
    private boolean isStuck = false;
    private int despawnTimer = 1200; // Увеличено время жизни до 60 сек
    private boolean returning = false;
    private ItemStack internalStack = ItemStack.EMPTY;

    // Конструктор для регистрации
    public ThrownAxeEntity(EntityType<? extends ThrownAxeEntity> type, Level level) {
        super(type, level);
    }

    // Основной конструктор броска
    public ThrownAxeEntity(Level level, LivingEntity owner, ItemStack stack, float damage) {
        super(ModEntities.THROWN_AXE.get(), owner, level);
        
        // Клонируем стек
        this.internalStack = stack.copy();
        this.internalStack.setCount(1);
        this.setItem(this.internalStack);
        this.baseDamage = damage;

        if (owner instanceof Player player && !level.isClientSide) {
            // === КРИТИЧЕСКИЙ ФИКС ФАНТОМНОГО ИСЧЕЗНОВЕНИЯ ===
            // Если предмет на кулдауне — мы вообще не должны были сюда попасть.
            // Принудительно отменяем бросок, если кулдаун еще активен.
            if (player.getCooldowns().isOnCooldown(stack.getItem())) {
                this.discard(); 
                return;
            }

            // Интеграция стамины
            FatigueSystem.addFatigue(player, 5.0f);
            
            level.playSound(null, player.getX(), player.getY(), player.getZ(), 
                SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.PLAYERS, 0.8F, 0.7F);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STUCK, false);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.IRON_AXE;
    }

    /**
     * ГЕТТЕР ДЛЯ РЕНДЕРЕРА (Symbol Fix)
     */
    public boolean isStuckInBlock() {
        return this.entityData.get(DATA_STUCK);
    }

    /**
     * ЗАЩИТА ОТ УРОНА (Чтобы молния не удаляла сущность)
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE) || source.is(DamageTypes.LIGHTNING_BOLT)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    /**
     * ГЛАВНЫЙ ЦИКЛ ОБНОВЛЕНИЯ
     */
    @Override
    public void tick() {
        // Логика когда топор уже в стене
        if (this.isStuckInBlock() && !returning) {
            this.handleStaticStuckLogic();
            return;
        }

        // Базовый тик полета (коллизии)
        super.tick();

        // Расчет физики полета
        if (!this.isStuckInBlock() && !returning) {
            this.applyCustomFlightPhysics();
        }

        // Логика возвращения (Loyalty)
        if (returning) {
            this.handleLoyaltyReturnMovement();
        }
    }

    /**
     * УЛУЧШЕННАЯ БАЛЛИСТИКА: 16 блоков ровно (32 тика), затем падение.
     */
    private void applyCustomFlightPhysics() {
        flightTicks++;
        Vec3 motion = this.getDeltaMovement();

        // Динамический расчет гравитации
        double gravity;
        if (flightTicks < 32) {
            gravity = 0.001D; 
            this.setDeltaMovement(motion.x * 0.999D, motion.y - gravity, motion.z * 0.999D);
        } else {
            gravity = Math.min(0.12D, 0.01D + (flightTicks - 32) * 0.02D);
            this.setDeltaMovement(motion.x * 0.96D, motion.y - gravity, motion.z * 0.96D);
        }
        
        // Визуальные эффекты Громовержца в полете
        if (this.level().isThundering() && this.getChannelingLevel() > 0) {
            if (this.level() instanceof ServerLevel sLevel) {
                sLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY(), this.getZ(), 2, 0.1, 0.1, 0.1, 0.01);
            }
        }

        // Износ в полете (раз в 4 тика)
        if (!this.level().isClientSide && flightTicks % 4 == 0) {
            this.applyAxeDurabilityDamage(1);
        }

        // Защита от бесконечного полета
        if (this.getY() < this.level().getMinBuildHeight() || flightTicks > 150) {
            this.initiateReturnOrDrop();
        }
    }

    /**
     * Логика застрявшего в блоке предмета
     */
    private void handleStaticStuckLogic() {
        this.despawnTimer--;
        
        // Проверка на подбор игроком (владельцем)
        if (!this.level().isClientSide && this.getOwner() instanceof ServerPlayer player) {
            if (this.getBoundingBox().inflate(1.2).intersects(player.getBoundingBox())) {
                this.securePickup(player);
            }
        }

        // Если время вышло - выпадаем предметом
        if (this.despawnTimer <= 0) {
            this.dropAsItemEntity();
            this.discard();
        }
    }

    /**
     * Перемещение при возврате (Loyalty)
     */
    private void handleLoyaltyReturnMovement() {
        this.setNoGravity(true);
        this.entityData.set(DATA_STUCK, false); // Снимаем статус "застрял" при начале возврата
        this.isStuck = false;
        Entity owner = this.getOwner();

        if (owner instanceof Player player && player.isAlive()) {
            // Точка назначения - грудь игрока
            Vec3 targetPos = player.getEyePosition().subtract(0, 0.4, 0);
            Vec3 vectorToPlayer = targetPos.subtract(this.position());
            
            // Скорость возврата выше скорости броска
            this.setDeltaMovement(vectorToPlayer.normalize().scale(0.85D));

            if (vectorToPlayer.length() < 1.5D) {
                this.handlePlayerCollisionOnReturn(player);
            }
        } else {
            // Если игрок умер или вышел - падаем
            this.returning = false;
            this.setNoGravity(false);
            this.setDeltaMovement(Vec3.ZERO);
        }
    }

    /**
     * Безопасный подбор: исключает исчезновение предмета
     */
    private void securePickup(Player player) {
        if (this.isRemoved()) return;

        ItemStack stackToGive = this.internalStack.copy();
        
        // Если инвентарь принял предмет
        if (player.getInventory().add(stackToGive)) {
            // СБРОС КУЛДАУНА
            player.getCooldowns().removeCooldown(stackToGive.getItem());
            
            this.level().playSound(null, player.getX(), player.getY(), player.getZ(), 
                    SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
            this.discard();
        } else {
            // Если инвентарь полон - дропаем под ноги, не удаляя сущность бесследно
            this.dropAsItemEntity();
            this.discard();
        }
    }

    private void handlePlayerCollisionOnReturn(Player player) {
        Vec3 lookDirection = player.getViewVector(1.0F);
        Vec3 toAxeDirection = this.position().subtract(player.position()).normalize();
        
        // Попытка поймать (нужно смотреть в сторону топора)
        if (lookDirection.dot(toAxeDirection) > 0.22D) {
            this.securePickup(player);
        } else {
            // Не поймал - получил урон рукояткой (50% урона)
            player.hurt(this.damageSources().thrown(this, this.getOwner()), this.calculateAxeDamage() * 0.5F);
            this.dropAsItemEntity();
            this.discard();
        }
    }

    private float calculateAxeDamage() {
        // Урон снижается на 4% за каждые 10 тиков полета
        float falloff = 1.0F - (flightTicks * 0.04F);
        return Math.max(baseDamage * falloff, 2.0F);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (this.level().isClientSide) return;

        Entity target = result.getEntity();
        if (target == this.getOwner()) return;

        // 1. Взаимодействие со щитом
        if (target instanceof LivingEntity livingTarget && livingTarget.isBlocking()) {
            this.handleShieldCollision(livingTarget, result.getLocation());
        }

        // 2. Расчет урона и Хедшот
        float finalDamage = this.calculateAxeDamage();
        double headLevel = target.getY() + (target.getBbHeight() * 0.82D);
        
        if (result.getLocation().y > headLevel) {
            finalDamage *= 1.5F; // Критический множитель в голову
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), 
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.3F);
        }

        // 3. Логика Громовержца (Channeling)
        this.trySpawnLightning(target);

        // Наносим урон
        target.hurt(this.damageSources().thrown(this, this.getOwner()), finalDamage);
        
        // После удара либо возвращаемся, либо выпадаем
        this.initiateReturnOrDrop();
    }

    private void trySpawnLightning(Entity target) {
        if (this.getChannelingLevel() > 0 && this.level().isThundering()) {
            if (this.level() instanceof ServerLevel sLevel && sLevel.canSeeSky(target.blockPosition())) {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sLevel);
                if (bolt != null) {
                    bolt.moveTo(target.position());
                    bolt.setCause((ServerPlayer) this.getOwner());
                    sLevel.addFreshEntity(bolt);
                    
                    // Эффект грома и защита от самовозгорания
                    this.setRemainingFireTicks(0);
                    this.clearFire();
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(), 
                            SoundEvents.TRIDENT_THUNDER, SoundSource.WEATHER, 5.0F, 1.0F);
                }
            }
        }
    }

    private void handleShieldCollision(LivingEntity target, Vec3 pos) {
        // Искры при ударе в щит
        if (this.level() instanceof ServerLevel sLevel) {
            sLevel.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 20, 0.2, 0.2, 0.2, 0.4);
            sLevel.sendParticles(ParticleTypes.LAVA, pos.x, pos.y, pos.z, 8, 0.1, 0.1, 0.1, 0.1);
        }

        if (target instanceof Player player) {
            // Пробитие щита (КД 5 секунд)
            player.getCooldowns().addCooldown(target.getUseItem().getItem(), 100);
            target.stopUsingItem();
            
            this.level().broadcastEntityEvent(target, (byte) 30);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), 
                    SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
        }
    }

   @Override
    protected void onHitBlock(BlockHitResult result) {
        if (this.level().isClientSide) return;

        if (this.getLoyaltyLevel() > 0) {
            this.returning = true;
        } else {
            this.isStuck = true;
            this.entityData.set(DATA_STUCK, true);
            
            // Сдвигаем точку фиксации чуть назад от поверхности (на 0.1), 
            // чтобы рендер не "тонул" в блоке
            Vec3 motion = this.getDeltaMovement().normalize();
            Vec3 hitPos = result.getLocation().subtract(motion.scale(0.1));
            
            this.setPos(hitPos.x, hitPos.y, hitPos.z);
            
            this.setDeltaMovement(Vec3.ZERO);
            this.setNoGravity(true);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), 
                    SoundEvents.TRIDENT_HIT_GROUND, SoundSource.PLAYERS, 1.0F, 0.75F);
        }
    }

    private int getLoyaltyLevel() {
        return EnchantmentHelper.getItemEnchantmentLevel(
            this.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOYALTY), 
            this.getItem()
        );
    }

    private int getChannelingLevel() {
        return EnchantmentHelper.getItemEnchantmentLevel(
            this.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.CHANNELING), 
            this.getItem()
        );
    }

    private void initiateReturnOrDrop() {
        if (this.getLoyaltyLevel() > 0) {
            this.returning = true;
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), 
                    SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 1.0F, 1.0F);
        } else {
            this.dropAsItemEntity();
            this.discard();
        }
    }

    private void dropAsItemEntity() {
        if (!this.level().isClientSide && !this.internalStack.isEmpty()) {
            ItemEntity entityItem = new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), this.internalStack.copy());
            entityItem.setNoPickUpDelay();
            
            // Если на топоре чары - он не горит как предмет
            if (this.getChannelingLevel() > 0 || this.getLoyaltyLevel() > 0) {
                entityItem.setInvulnerable(true);
            }
            
            this.level().addFreshEntity(entityItem);
            // Очищаем внутренний стек, чтобы избежать дубликатов при ошибках
            this.internalStack = ItemStack.EMPTY;
        }
    }

    private void applyAxeDurabilityDamage(int amount) {
        if (this.internalStack.isDamageableItem()) {
            this.internalStack.setDamageValue(this.internalStack.getDamageValue() + amount);
            if (this.internalStack.getDamageValue() >= this.internalStack.getMaxDamage()) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), 
                        SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
                this.level().broadcastEntityEvent(this, (byte) 3);
                this.discard();
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("AxeBaseDamage", this.baseDamage);
        tag.putInt("AxeFlightTicks", this.flightTicks);
        tag.putBoolean("AxeIsStuck", this.isStuckInBlock());
        tag.putBoolean("AxeIsReturning", this.returning);
        if (!this.internalStack.isEmpty()) {
            tag.put("AxeStackData", this.internalStack.save(this.level().registryAccess()));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.baseDamage = tag.getFloat("AxeBaseDamage");
        this.flightTicks = tag.getInt("AxeFlightTicks");
        boolean stuck = tag.getBoolean("AxeIsStuck");
        this.isStuck = stuck;
        this.entityData.set(DATA_STUCK, stuck);
        this.returning = tag.getBoolean("AxeIsReturning");
        if (tag.contains("AxeStackData")) {
            this.internalStack = ItemStack.parse(this.level().registryAccess(), tag.getCompound("AxeStackData")).orElse(ItemStack.EMPTY);
            this.setItem(this.internalStack);
        }
    }
}