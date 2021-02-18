package org.mineacademy.fo.display;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.model.Tuple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple animation class for running text animations for players.
 * <p>
 * Extend it and override the abstract methods to customize it. Then create a new instance
 * and call {@link #launch(boolean)}.
 */
public abstract class SimpleAnimation extends BukkitRunnable {
	/**
	 * The processed animations for the animation types, cached for performance saving
	 */
	private static final Map<String, List<Tuple<Integer, String>>> processedFrameLists = new HashMap<>();

	/**
	 * How many seconds later should we start this animation after calling {@link #launch(boolean)}?
	 */
	private final int startDelay;

	/**
	 * The frames cached in a time and text key-value pairs
	 */
	private final List<Tuple<Integer, String>> frames = new ArrayList<>();

	/**
	 * Should this animation run only once?
	 */
	private final boolean runOnce;

	/**
	 * Creates a new animation from the given frames with no start delay, that runs forever
	 *
	 * @param name
	 * @param frames
	 */
	protected SimpleAnimation(final String name, final List<String> frames) {
		this(name, frames, false, 0);
	}

	/**
	 * Creates a new animation from the given frames
	 *
	 * @param name       the name of this animation, used for caching
	 * @param frames     the raw frames to process
	 * @param runOnce    if the animation should run only once or forever
	 * @param startDelay the delay of starting after {@link #launch(boolean)}
	 */
	protected SimpleAnimation(final String name, final List<String> frames, final boolean runOnce, final int startDelay) {
		this.startDelay = startDelay;
		this.runOnce = runOnce;

		// Process and cache the frames if not cached yet
		if (processedFrameLists.containsKey(name))
			this.frames.addAll(processedFrameLists.get(name));

		else {
			for (final String frame : frames) {
				final String[] split = frame.split("::");
				final int frameTime = Integer.parseInt(split[0]);

				this.frames.add(new Tuple<>(frameTime, split[1]));
			}

			processedFrameLists.put(name, this.frames);
		}

		currentFrame = this.frames.get(0);
	}

	// --------------------------------------------------------------------
	// Running animation data
	// --------------------------------------------------------------------

	private int timer = 0, index = 0, animationFinished = 0;

	/**
	 * The frame currently being selected.
	 * <p>
	 * You can access it and show it to the player in {@link #onNextFrame()}
	 */
	protected Tuple<Integer, String> currentFrame;

	@Override
	public void run() {
		if (canCancel() || (runOnce && animationFinished > 0)) {
			cancel();
			return;
		}

		// The timer reached the next frame
		if (timer == currentFrame.getKey()) {
			onNextFrame();

			if (index < frames.size() - 1)
				currentFrame = frames.get(++index);
		}

		// If the animation is over, start it again
		if (timer + 1 > Common.last(frames).getKey()) {
			timer = 0;
			index = 0;
			animationFinished++;

			currentFrame = frames.get(0);

		} else
			timer++;
	}

	/**
	 * When we should end this animation?
	 *
	 * @return true if the animation should be cancelled
	 */
	protected abstract boolean canCancel();

	/**
	 * This method is called when the timer hits a frame's time.
	 */
	protected abstract void onNextFrame();

	/**
	 * Runs the animation.
	 *
	 * @param async if true, the animation will run asynchronously
	 * @return
	 */
	public final BukkitTask launch(final boolean async) {
		if (async)
			return Common.runTimerAsync(startDelay, 1, this);

		return Common.runTimer(startDelay, 1, this);
	}
}
