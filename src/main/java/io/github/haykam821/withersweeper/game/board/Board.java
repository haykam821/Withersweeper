package io.github.haykam821.withersweeper.game.board;

import java.util.Random;

import io.github.haykam821.withersweeper.game.field.Field;
import io.github.haykam821.withersweeper.game.field.FieldVisibility;
import io.github.haykam821.withersweeper.game.field.MineField;
import io.github.haykam821.withersweeper.game.field.NumberField;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import xyz.nucleoid.plasmid.game.map.template.MapTemplate;

public class Board {
	private final BoardConfig config;
	private final Field[][] fields;

	public Board(BoardConfig config, Random random) {
		this.config = config;

		this.fields = new Field[this.config.x][this.config.z];
		for (int x = 0; x < this.config.x; x++) {
			for (int z = 0; z < this.config.z; z++) {
				this.setField(x, z, new NumberField(0));
			}
		}

		if (this.config.mines > (this.config.x * this.config.z)) {
			throw new IllegalStateException("Cannot have more mines than fields");
		}

		for (int index = 0; index < this.config.mines; index++) {
			int x = random.nextInt(this.config.x);
			int z = random.nextInt(this.config.z);

			// Prevent two mines from generating on each other
			Field field = this.getField(x, z);
			if (field instanceof MineField) {
				index--;
				continue;
			}

			this.setField(x, z, new MineField());

			// Increase the value of neighboring fields
			this.increaseField(x - 1, z - 1);
			this.increaseField(x, z - 1);
			this.increaseField(x + 1, z - 1);

			this.increaseField(x - 1, z);
			this.increaseField(x + 1, z);

			this.increaseField(x - 1, z + 1);
			this.increaseField(x, z + 1);
			this.increaseField(x + 1, z + 1);
		}
	}
	
	public void setField(int x, int z, Field field) {
		this.fields[z][x] = field;
	}

	public Field getField(int x, int z) {
		if (!this.isValidPos(x, z)) return null;
		return this.fields[z][x];
	}

	public void increaseField(int x, int z) {
		Field field = this.getField(x, z);
		if (field instanceof NumberField) {
			this.setField(x, z, ((NumberField) field).increaseValue());
		}
	}

	public boolean isValidPos(int x, int z) {
		return x >= 0 && z >= 0 && x < this.config.x && z < this.config.z;
	}

	public int getRemainingFlags() {
		int remainingFlags = this.config.mines;
		for (int x = 0; x < this.config.x; x++) {
			for (int z = 0; z < this.config.z; z++) {
				Field field = this.getField(x, z);
				if (field.getVisibility() == FieldVisibility.FLAGGED) {
					remainingFlags -= 1;
				}
			}
		}

		return remainingFlags;
	}

	public boolean isCompleted() {
		for (int x = 0; x < this.config.x; x++) {
			for (int z = 0; z < this.config.z; z++) {
				Field field = this.getField(x, z);
				if (field.getVisibility() != FieldVisibility.UNCOVERED && !(field instanceof MineField)) {
					return false;
				}
			}
		}

		return true;
	}

	public void build(MapTemplate template) {
		BlockPos.Mutable pos = new BlockPos.Mutable();

		for (int x = 0; x < this.config.x; x++) {
			for (int z = 0; z < this.config.z; z++) {
				pos.set(x, 0, z);

				Field field = this.getField(x, z);
				if (field != null) {
					template.setBlockState(pos, field.getCoveredBlockState());
				}
			}
		}
	}

	public void build(WorldAccess world) {
		BlockPos.Mutable pos = new BlockPos.Mutable();

		for (int x = 0; x < this.config.x; x++) {
			for (int z = 0; z < this.config.z; z++) {
				pos.set(x, 0, z);

				Field field = this.getField(x, z);
				if (field != null) {
					world.setBlockState(pos, field.getCoveredBlockState(), 2);
				}
			}
		}
	}
}