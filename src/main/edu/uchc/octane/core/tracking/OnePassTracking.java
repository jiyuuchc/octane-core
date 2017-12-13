package edu.uchc.octane.core.tracking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.ListIterator;

import edu.uchc.octane.core.utils.HData;

/**
 * Tracking module to generate trajectories
 * @author Ji-Yu
 *
 */
public class OnePassTracking {

	public class Trajectory extends ArrayList<HData> {
		int lastFrame;
	};

	Collection<? extends HData>[] localizations; //array index is frame number

//	private Integer [][] backwardBonds;
//	private Bond [][] forwardBonds;
//	private boolean [] isTrackedParticle;
//	private Trajectory wasted_;

	// private double maxDisplacement;
	double maxDisplacement2;
	int maxBlinking;
	// private double lowerBound;

//	class Bond implements Comparable<Bond> {
//		int bondTo;
//		double bondLength;
//		@Override
//		public int compareTo(Bond o) {
//			return (int) Math.signum(bondLength - o.bondLength);
//		}
//	}

	public OnePassTracking(double maxDisplacement, int maxBlinking) {
		this.maxBlinking = maxBlinking;
		maxDisplacement2 = maxDisplacement * maxDisplacement;
	}

//	protected void clusterAndOptimize(int seed) {
//		Vector<Integer> headList = new Vector<Integer>();
//		Vector<Integer> tailList = new Vector<Integer>();
//
//		headList.add(seed);
//		ListIterator<Integer> itHead = headList.listIterator();
//		ListIterator<Integer> itTail = tailList.listIterator();
//		itHead.next();
//
//		while ( itHead.hasPrevious()) {
//			while (itHead.hasPrevious()) {
//				int trackIdx = itHead.previous();
//				for (int j = 0; j < forwardBonds[trackIdx].length; j ++) {
//					int tail = forwardBonds[trackIdx][j].bondTo;
//					if ( !isTrackedParticle[tail] && ! tailList.contains(tail)) {
//						itTail.add(tail); // surprising, this add _before_ the cursor.
//					}
//				}
//			}
//			//lastHeadListEnd = headList.size();
//
//			while (itTail.hasPrevious()) {
//				int posIdx = itTail.previous();
//				for (int j = 0; j < backwardBonds[posIdx].length; j++) {
//					int head = backwardBonds[posIdx][j];
//					if (forwardBonds[head] != null && ! headList.contains(head)) {
//						itHead.add(head);
//					}
//				}
//			}
//			//lastTailListEnd = tailList.size();
//		}
//
//		optimizeSubnetwork(headList, tailList);
//	}
//
//	protected void optimizeSubnetwork(Vector<Integer> headList, Vector<Integer> tailList) {
//		double bestDistanceSum = Double.MAX_VALUE;
//		int curBondIdx = -1;
//		double curDistanceSum = 0;
//		Stack<Integer> stack = new Stack<Integer>();
////		Stack<Integer> occupiedTails = new Stack<Integer>();
//		HashSet<Integer> occupiedTails = new HashSet<Integer>(headList.size());
//		Stack<HashSet<Integer>> tailStack = new Stack<HashSet<Integer>>();
//		Stack<Double> distanceStack = new Stack<Double>();
//		Stack<Integer> stack_c = null;
//		int trackIdx;
//		int tail;
//		double nextBondLength;
//		double [] minExtraLength = new double[headList.size()];
//
////		if (headList.size() + tailList.size() > 400) {
////			IJ.log("Optimizing a very large network: " + headList.size() + "," + tailList.size() + ". This might take for ever.");
////		}
//
//		double m = 0;
//		for (int i = headList.size()-1 ; i >=0; i--) {
//			minExtraLength[i] = m;
//			m += forwardBonds[headList.get(i)][0].bondLength;
//		}
//
//		while (true) {
//			trackIdx = headList.get(stack.size());
//
//			// try next possible bond
//			while (curBondIdx  < forwardBonds[trackIdx].length) {
//				curBondIdx ++ ;
//
//				// test if this is a good bond
//				if (curBondIdx == forwardBonds[trackIdx].length) { //special case, no bonding
//					if (curDistanceSum + maxDisplacement >= bestDistanceSum) {
//						break;
//					}
//					//curDistanceSum += threshold_;
//					nextBondLength = maxDisplacement;
//					tail = -1;
//				} else {
//					tail = forwardBonds[trackIdx][curBondIdx].bondTo;
//					if (occupiedTails.contains(tail)) {
//						continue; //next bond
//					}
//
//					nextBondLength = forwardBonds[trackIdx][curBondIdx].bondLength;
//
//					if (curDistanceSum + nextBondLength + minExtraLength[stack.size()] >= bestDistanceSum) {
//						break; //fail
//					}
//					//curDistanceSum += bondLengths_.get(trackIdx).get(curBondIdx);
//				}
//
//				//looks ok, push to stack
//				if (stack.size() < headList.size()-1) {
//					stack.push(curBondIdx);
//					tailStack.push(new HashSet<Integer>(occupiedTails));
//					occupiedTails.add(tail);
//					distanceStack.push(curDistanceSum);
//					curBondIdx = -1;
//					curDistanceSum += nextBondLength;
//					trackIdx = headList.get(stack.size());
//				} else { // unless this is the last element
//					bestDistanceSum = curDistanceSum + nextBondLength;
//					stack.push(curBondIdx);
//					stack_c = (Stack<Integer>) stack.clone();
//					stack.pop();
//					break;
//				}
//			}
//
//			if (stack.size() > 0) {
//				curBondIdx = stack.pop();
//				occupiedTails = tailStack.pop();
//				curDistanceSum = distanceStack.pop();
//			} else { // finished here
//				break;
//			}
//
//		} //while
//
//		// got best route. creating new bonds
//		for (int i = 0; i < stack_c.size(); i ++) {
//			trackIdx = headList.get(i);
//			int bondIdx = stack_c.get(i);
//			if (bondIdx < forwardBonds[trackIdx].length) {
//				int bondTo = forwardBonds[trackIdx][bondIdx].bondTo;
//				Localization n = localizations[curFrame][bondTo];
//				activeTracks.get(trackIdx).add(n); //might be slow if tracks_ is too big
//				assert(isTrackedParticle[bondTo] == false);
//				isTrackedParticle[bondTo] = true;
//				backwardBonds[bondTo] = null;
//			}
//			forwardBonds[trackIdx] = null;
//		}
//	}
//
//	protected void buildAllPossibleBonds() {
//		forwardBonds = new Bond[activeTracks.size()][];
//		backwardBonds = new Integer[localizations[curFrame].size()][];
//		isTrackedParticle = new boolean[localizations[curFrame].size()];
//		HashMap<> bonds
//
//		for (Trajectory track : activeTracks) {
//			HData fromPoint = track.get(track.size() - 1);
//			int nBonds = 0;
//			for (HData toPoint : localizations[curFrame]) {
//				double dist = fromPoint.sqDistance(toPoint);
//				if (dist <= maxDisplacement2) { // don't miss the = sign
//					Bond b = new Bond();
//					b.bondLength = d;
//					b.bondTo = j;
//					bonds[nBonds++] = b;
//					backBonds[j].add(id);
//				}
//			}
//
//
//		}
//		// calculated all possible bonds
//		Iterator <Trajectory> it = activeTracks.iterator();
//		while( it.hasNext() ) {
//			int id = it.nextIndex();
//			Localization trackHead = it.next().lastElement();
//
//			nBonds = 0;
//
//			for (int j = 0; j < localizations[curFrame].size(); j ++) {
//				double d = trackHead.distance2(localizations[curFrame][j]);
//				if (d <= maxDisplacement2) { // don't miss the = sign
//					Bond b = new Bond();
//					b.bondLength = d;
//					b.bondTo = j;
//					bonds[nBonds++] = b;
//					backBonds[j].add(id);
//				}
//			}
//
//			forwardBonds[id] = Arrays.copyOf(bonds, nBonds);
//			if (nBonds > 1) {
//				Arrays.sort(forwardBonds[id]);
//				// we won't do network search if the shortest link is small enough
//				if (forwardBonds[id][0].bondLength <= lowerBound) {
//					forwardBonds[id] = Arrays.copyOf(forwardBonds[id], 1);
//				}
//			}
//		}
//
//		// create backward bonds
//		for (int i = 0; i < backBonds.length; i ++) {
//			backwardBonds[i] = new Integer[backBonds[i].size()];
//			backBonds[i].toArray(backwardBonds[i]);
//		}
//
//	}
//
//	protected void trivialBonds() {
//		// search all trivial bonds
//		for (int i = 0; i < forwardBonds.length; i++ ) {
//			if (forwardBonds[i].length == 1) {
//				int bondTo = forwardBonds[i][0].bondTo;
//				if (backwardBonds[bondTo].length == 1) {
//					// trivial bond
//					forwardBonds[i] = null;
//					backwardBonds[bondTo] = null;
//					Localization n = localizations[curFrame][bondTo];
//					activeTracks.get(i).add(n);
//					isTrackedParticle[bondTo] = true;
//					continue;
//				}
//			}
//
//			for (int j = 0; j < forwardBonds[i].length; j++) {
//				forwardBonds[i][j].bondLength = Math.sqrt(forwardBonds[i][j].bondLength);
//			}
//		}
//	} // TrivialBonds()

	// link points into activeTracks; remove connected one from points.
	void connect(Iterable<Trajectory> activeTracks, LinkedList<HData> points, int curFrame) {
		for (Trajectory track : activeTracks) {
			HData fromPoint = track.get(track.size()-1);
			for (ListIterator<HData> it = points.listIterator(); it.hasNext(); ) {
				HData point = it.next();
				if ( fromPoint.sqDistance(point) < maxDisplacement2 ) {
					track.add(point);
					track.lastFrame = curFrame;
					it.remove();
					break;
				}
			}
		}
	}

	/**
	 * Create trajectories.
	 *
	 * @return the trajectories
	 */
	public ArrayList<Trajectory> doTracking(Collection<? extends HData>[] localizations) {

		this.localizations  = localizations;
		LinkedList<Trajectory> activeTracks = new LinkedList<Trajectory>();
		ArrayList<Trajectory> trajectories = new ArrayList<Trajectory>();

		//initial track # = first frame particle #
		for (HData point : localizations[0]) {
			Trajectory t = new Trajectory();
			t.add(point);
			t.lastFrame = 0;
			activeTracks.add(t);
		}

		int curFrame = 0;
		while ( ++ curFrame < localizations.length) {
//			buildAllPossibleBonds();
//			trivialBonds();
//			for ( int i = 0; i < forwardBonds.length; i ++) {
//				if (forwardBonds[i] != null && forwardBonds[i].length > 0) {
//					clusterAndOptimize(i);
//				}
//			}

			LinkedList<HData> points = new LinkedList<HData>();
			points.addAll(localizations[curFrame]);
			connect(activeTracks, points, curFrame);

			//remove all tracks that has been lost for too long
			//int firstPos = xytData_.getFirstOfFrame(curFrame_);
			for (ListIterator<Trajectory> it = activeTracks.listIterator(); it.hasNext(); ) {
				Trajectory track = it.next();
				if (curFrame - track.lastFrame >= maxBlinking) {
					it.remove();
					trajectories.add(track);
				}
			}

			//add new particles into the track list
			for (HData point : points) {
				Trajectory t = new Trajectory();
				t.add(point);
				t.lastFrame = curFrame;
				activeTracks.add(t);
			}
		} //while

		trajectories.addAll(activeTracks);
		activeTracks = null;

		return trajectories;
	} //doTracking
}
