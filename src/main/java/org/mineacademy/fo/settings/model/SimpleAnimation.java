package org.mineacademy.fo.settings.model;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.model.Tuple;
import org.mineacademy.fo.plugin.SimplePlugin;

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
	 * @param identifier the identifier of this animation, used for caching processed frames
	 * @param frames     the raw frames to process, supports & colors
	 * @param runOnce    if the animation should run only once
	 * @param startDelay the delay of starting after {@link #launch(boolean)}
	 */
	protected SimpleAnimation(final String identifier, List<String> frames, final boolean runOnce, final int startDelay) {
		this.startDelay = startDelay;
		this.runOnce = runOnce;

		// Process and cache the frames if not cached yet
		if (processedFrameLists.containsKey(identifier))
			this.frames.addAll(processedFrameLists.get(identifier));

		else {
			frames = Common.colorize(frames);

			for (final String frame : frames) {
				final String[] split = frame.split("::");
				final int frameTime = Integer.parseInt(split[0]);

				this.frames.add(new Tuple<>(frameTime, split[1]));
			}

			processedFrameLists.put(identifier, this.frames);
		}

		currentFrame = this.frames.get(0);
	}

	// --------------------------------------------------------------------
	// Backend
	// --------------------------------------------------------------------

	/**
	 * The task of the running animation
	 */
	private BukkitTask task;

	/**
	 * The time elapsed since the animation started, resets each time the
	 * animation starts over
	 */
	private int timer = 0;

	/**
	 * The index of the current animation frame
	 */
	private int index = 0;

	/**
	 * Indicates how many times the animation has finished, used for stopping
	 * it if it has to run only once
	 */
	private int animationFinished = 0;

	/**
	 * The frame currently being selected.
	 * <p>
	 * You can access it and show it to the player in {@link #onNextFrame()}
	 */
	protected Tuple<Integer, String> currentFrame;

	/**
	 * @see org.bukkit.scheduler.BukkitRunnable#run()
	 */
	@Override
	public void run() {
		// Stop if we can or if the animation has finished for the first time
		if (canCancel() || (runOnce && animationFinished > 0)) {
			cancel();

			return;
		}

		// The timer reached the next frame
		if (timer == currentFrame.getKey()) {
			try {
				onNextFrame();

			} catch (Throwable t) {
				Common.error(t,
						"Error while ticking animation " + getClass() + ", stopping",
						"Current frame: " + currentFrame);
				cancel();

				return;
			}

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
	 * Called when the timer hits a frame's time.
	 */
	protected abstract void onNextFrame();

	/**
	 * Called when this animation stops
	 */
	protected void onEnd() {
	}

	// -----------------------------------------------------------------------------------
	// Animation management
	// -----------------------------------------------------------------------------------

	/**
	 * Runs the animation.
	 *
	 * @param async if true, the animation will run asynchronously
	 * @return
	 */
	public final BukkitTask launch(final boolean async) {
		if (async)
			return task = this.runTaskTimerAsynchronously(SimplePlugin.getInstance(), startDelay, 1);

		return task = this.runTaskTimer(SimplePlugin.getInstance(), startDelay, 1);
	}

	/**
	 * Stops this animation, throwing an error if it wasn't running
	 */
	public final void cancel() {
		Valid.checkBoolean(task != null && task.getTaskId() != -1, "Animation " + getClass() + " is not running");

		try {
			onEnd();

		} catch (Throwable t) {
			Common.error(t, "Error while cancelling animation " + getClass() + ", task id: " + task.getTaskId());
		}

		task.cancel();

		// Reset data for running
		timer = 0;
		index = 0;
		animationFinished = 0;
		currentFrame = this.frames.get(0);
	}
}
