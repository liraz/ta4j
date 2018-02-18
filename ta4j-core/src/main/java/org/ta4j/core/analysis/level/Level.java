package org.ta4j.core.analysis.level;

import java.io.Serializable;

/**
 * This class represents a support / resistance level.
 *
 * @author PRITESH
 *
 */
public class Level implements Serializable, Comparable<Level> {

	private static final long serialVersionUID = -7561265699198045328L;

	private final LevelType type;
	private final float level, strength;

	public Level(final LevelType type, final float level) {
		this(type, level, 0f);
	}

	public Level(final LevelType type, final float level, final float strength) {
		super();

		this.type = type;
		this.level = level;
		this.strength = strength;
	}

	public final LevelType getType() {
		return this.type;
	}

	public final float getLevel() {
		return this.level;
	}

	public final float getStrength() {
		return this.strength;
	}

	@Override
	public String toString() {
		return "Level [type=" + this.type + ", level=" + this.level
				+ ", strength=" + this.strength + "]";
	}

	@Override
	public int compareTo(Level o) {
		return Float.compare(getLevel(), o.getLevel());
	}
}
