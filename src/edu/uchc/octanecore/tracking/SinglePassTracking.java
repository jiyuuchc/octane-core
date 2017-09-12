package edu.uchc.octanecore.tracking;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Stack;
import java.util.Vector;

import edu.uchc.octanecore.datasource.Localization;
import edu.uchc.octanecore.datasource.LocalizationDataset;

/**
 * Tracking module to generate trajectories
 * @author Ji-Yu
 *
 */
public class SinglePassTracking {
	
	public class Trajectory extends Vector<Localization> {
		private static final long serialVersionUID = -948641768704241620L;	
	};

	// TrajDataset dataset_;
	protected Vector<Trajectory> trajectories;
	protected Localization[][] localizations; //nodes_[frame][#]

	private Integer [][] backwardBonds;
	private Bond [][] forwardBonds;
	private boolean [] isTrackedParticle; 
	private LinkedList<Trajectory> activeTracks;	
	// private Trajectory wasted_;

	private double maxDisplacement;
	private double maxDisplacement2;
	private int maxBlinking;
	private double lowerBound;
	private int curFrame;

	class Bond implements Comparable<Bond> {
		int bondTo;
		double bondLength;
		@Override
		public int compareTo(Bond o) {
			return (int) Math.signum(bondLength - o.bondLength); 
		}
	}

//	/**
//	 * Constructor
//	 * @param dataset The dataset
//	 */
//	public TrackingModule(LocalizationDataset dataset) {
//
//		this.nodes_  = dataset.toArrays();
//	
//	}
	
	public SinglePassTracking(double maxDisplacement, int maxBlinking) {

		this(maxDisplacement, maxBlinking, 0);

	}
	
	public SinglePassTracking(double maxDisplacement, int maxBlinking, double lowerBound) {

		this.maxDisplacement = maxDisplacement;
		this.maxBlinking = maxBlinking;
		this.lowerBound = lowerBound;
	
	}

	protected void clusterAndOptimize(int seed) {
		Vector<Integer> headList = new Vector<Integer>();
		Vector<Integer> tailList = new Vector<Integer>();

		headList.add(seed);
		ListIterator<Integer> itHead = headList.listIterator();
		ListIterator<Integer> itTail = tailList.listIterator();
		itHead.next();

		while ( itHead.hasPrevious()) {
			while (itHead.hasPrevious()) {
				int trackIdx = itHead.previous();
				for (int j = 0; j < forwardBonds[trackIdx].length; j ++) {
					int tail = forwardBonds[trackIdx][j].bondTo;
					if ( !isTrackedParticle[tail] && ! tailList.contains(tail)) {
						itTail.add(tail); // surprising, this add _before_ the cursor.
					}
				}
			}
			//lastHeadListEnd = headList.size();	

			while (itTail.hasPrevious()) {
				int posIdx = itTail.previous();
				for (int j = 0; j < backwardBonds[posIdx].length; j++) {
					int head = backwardBonds[posIdx][j];
					if (forwardBonds[head] != null && ! headList.contains(head)) {
						itHead.add(head);
					}
				}
			}
			//lastTailListEnd = tailList.size();
		}

		optimizeSubnetwork(headList, tailList);
	}

	protected void optimizeSubnetwork(Vector<Integer> headList, Vector<Integer> tailList) {
		double bestDistanceSum = Double.MAX_VALUE;
		int curBondIdx = -1;
		double curDistanceSum = 0;
		Stack<Integer> stack = new Stack<Integer>();
//		Stack<Integer> occupiedTails = new Stack<Integer>();
		HashSet<Integer> occupiedTails = new HashSet<Integer>(headList.size());
		Stack<HashSet<Integer>> tailStack = new Stack<HashSet<Integer>>();
		Stack<Double> distanceStack = new Stack<Double>();
		Stack<Integer> stack_c = null;
		int trackIdx; 
		int tail;
		double nextBondLength;
		double [] minExtraLength = new double[headList.size()];

//		if (headList.size() + tailList.size() > 400) {
//			IJ.log("Optimizing a very large network: " + headList.size() + "," + tailList.size() + ". This might take for ever.");
//		}
		
		double m = 0;
		for (int i = headList.size()-1 ; i >=0; i--) {
			minExtraLength[i] = m;
			m += forwardBonds[headList.get(i)][0].bondLength;
		}

		while (true) {
			trackIdx = headList.get(stack.size());

			// try next possible bond		
			while (curBondIdx  < forwardBonds[trackIdx].length) { 
				curBondIdx ++ ;

				// test if this is a good bond
				if (curBondIdx == forwardBonds[trackIdx].length) { //special case, no bonding
					if (curDistanceSum + maxDisplacement >= bestDistanceSum) {
						break;
					}
					//curDistanceSum += threshold_;
					nextBondLength = maxDisplacement;
					tail = -1;
				} else {
					tail = forwardBonds[trackIdx][curBondIdx].bondTo;
					if (occupiedTails.contains(tail)) {
						continue; //next bond
					}

					nextBondLength = forwardBonds[trackIdx][curBondIdx].bondLength;

					if (curDistanceSum + nextBondLength + minExtraLength[stack.size()] >= bestDistanceSum) {
						break; //fail
					}
					//curDistanceSum += bondLengths_.get(trackIdx).get(curBondIdx);
				}

				//looks ok, push to stack
				if (stack.size() < headList.size()-1) {
					stack.push(curBondIdx);
					tailStack.push(new HashSet<Integer>(occupiedTails));
					occupiedTails.add(tail);
					distanceStack.push(curDistanceSum);
					curBondIdx = -1;
					curDistanceSum += nextBondLength;
					trackIdx = headList.get(stack.size());
				} else { // unless this is the last element
					bestDistanceSum = curDistanceSum + nextBondLength;
					stack.push(curBondIdx);
					stack_c = (Stack<Integer>) stack.clone();
					stack.pop();
					break;
				}
			}

			if (stack.size() > 0) {
				curBondIdx = stack.pop();
				occupiedTails = tailStack.pop();
				curDistanceSum = distanceStack.pop();
			} else { // finished here
				break;
			}

		} //while

		// got best route. creating new bonds
		for (int i = 0; i < stack_c.size(); i ++) {			
			trackIdx = headList.get(i);
			int bondIdx = stack_c.get(i);
			if (bondIdx < forwardBonds[trackIdx].length) {
				int bondTo = forwardBonds[trackIdx][bondIdx].bondTo;
				Localization n = localizations[curFrame][bondTo];
				activeTracks.get(trackIdx).add(n); //might be slow if tracks_ is too big
				assert(isTrackedParticle[bondTo] == false);
				isTrackedParticle[bondTo] = true;
				backwardBonds[bondTo] = null;
			}
			forwardBonds[trackIdx] = null;
		}
	}


	protected void buildAllPossibleBonds() {
		forwardBonds = new Bond[activeTracks.size()][];
		//bondLengths_ = new double[activeTracks_.size()][];
		backwardBonds = new Integer[localizations[curFrame].length][];
		isTrackedParticle = new boolean[localizations[curFrame].length];
		
		Vector<Integer> [] backBonds = new Vector[localizations[curFrame].length];

//		for ( int i = 0; i < activeTracks_.size(); i ++) {
//			forwardBonds_[i] = new LinkedList<Integer>();
//			bondLengths_[i] = new LinkedList<Double>();
//		}
		for ( int i = 0; i < localizations[curFrame].length; i ++) {
			backBonds[i] = new Vector<Integer>();
		}

		Bond [] bonds = new Bond[localizations[curFrame].length];
		//double [] bondLengths = new double[nodes_[curFrame_].length];
		int nBonds = 0;

		// calculated all possible bonds
		ListIterator <Trajectory> it = activeTracks.listIterator();
		while( it.hasNext() ) {
			int id = it.nextIndex();
			Localization trackHead = it.next().lastElement();
			
			nBonds = 0;

			for (int j = 0; j < localizations[curFrame].length; j ++) {
				//				if (nodes_[curFrame_][j].residue > TrackingParameters.errorThreshold_) {
				double d = trackHead.distance2(localizations[curFrame][j]);
				if (d <= maxDisplacement2) { // don't miss the = sign
					Bond b = new Bond();
					b.bondLength = d;
					b.bondTo = j;
					bonds[nBonds++] = b;
					backBonds[j].add(id);
					//						forwardBonds_[id].add(j);
					//						bondLengths_[id].add(d);
					//						backwardBonds_[j].add(id);
				} 
				//				}
			}
			
			forwardBonds[id] = Arrays.copyOf(bonds, nBonds);
			if (nBonds > 1) {
				Arrays.sort(forwardBonds[id]);
				
				// we won't do network search if the shortest link is small enough
				if (forwardBonds[id][0].bondLength <= lowerBound) {
					forwardBonds[id] = Arrays.copyOf(forwardBonds[id], 1);
				}
			}
//			bondLengths_[id] = Arrays.copyOf(bondLengths, nBonds);
			
		}
		
		// create backward bons
		for (int i = 0; i < backBonds.length; i ++) {
			backwardBonds[i] = new Integer[backBonds[i].size()];
			backBonds[i].toArray(backwardBonds[i]);
		}

	}

	protected void trivialBonds() {
		// search all trivial bonds
		for (int i = 0; i < forwardBonds.length; i++ ) {
			if (forwardBonds[i].length == 1) {
				int bondTo = forwardBonds[i][0].bondTo;
				if (backwardBonds[bondTo].length == 1) {
					// trivial bond
					forwardBonds[i] = null;
					backwardBonds[bondTo] = null;
					Localization n = localizations[curFrame][bondTo];
					activeTracks.get(i).add(n);
					isTrackedParticle[bondTo] = true;
					continue;
				}
			} 

			for (int j = 0; j < forwardBonds[i].length; j++) {
				forwardBonds[i][j].bondLength = Math.sqrt(forwardBonds[i][j].bondLength);
//				bondLengths_[i][j]=Math.sqrt(bondLengths_[i][j]);
			}
		
		}
	} // TrivialBonds()


	/**
	 * Create trajectories.
	 *
	 * @return the trajectories
	 */
	public Vector<Trajectory> doTracking(LocalizationDataset dataset) {

		localizations  = dataset.toArrays();

		maxDisplacement2 = maxDisplacement * maxDisplacement;
		activeTracks = new LinkedList<Trajectory>();
		trajectories = new Vector<Trajectory>();
		// wasted_ = new Trajectory();

		//initial track # = first frame particle #  
		for (int i = 0; i < localizations[0].length; i ++ ) {
			Trajectory t;
			t = new Trajectory();
			t.add(localizations[0][i]);
			activeTracks.add(t);
		}

		curFrame = 1;
		while (curFrame < localizations.length) {
			
//			if (curFrame_ % 50 == 0 ) {
//				IJ.log("Frame " + curFrame_ + ", "
//						+ activeTracks_.size() + " active tracks, " 
//						+ trajectories_.size() + " stopped tracks."
//						+ nodes_[curFrame_].length + "new nodes");
//			}
//			IJ.showProgress(curFrame_, nodes_.length);

			buildAllPossibleBonds();
			trivialBonds();

			for ( int i = 0; i < forwardBonds.length; i ++) {
				if (forwardBonds[i] != null && forwardBonds[i].length > 0) {
					clusterAndOptimize(i);
				}
			}

			//remove all tracks that has been lost for too long
			//int firstPos = xytData_.getFirstOfFrame(curFrame_);
			Iterator<Trajectory> it = activeTracks.iterator(); 
			while ( it.hasNext() ) {
				Trajectory track = it.next();
				int frame = track.lastElement().frame;
				if (curFrame - frame >= maxBlinking) {
					it.remove();
					trajectories.add(track);
				} 
			}

			//add new particles into the track list
			for (int i = 0; i < localizations[curFrame].length; i++) {
				if (! isTrackedParticle[i]) {
					assert(backwardBonds[i] != null);
					Trajectory t;
					t = new Trajectory();
					t.add(localizations[curFrame][i]);
					activeTracks.add(t);
				}
			}

			curFrame ++;
		} //while

		// add all tracks to stoppedTracks list
		Iterator<Trajectory> it = activeTracks.iterator(); 
		while (it.hasNext()) {
			Trajectory track = it.next();
			trajectories.add(track);
		}

//		if (wasted_ != null && wasted_.size() > 0) {
//			wasted_.deleted = true;
//			trajectories_.add(wasted_);
//		}

		activeTracks.clear();
		activeTracks = null;
		localizations = null;
		//wasted_ = null;
		
		return trajectories;

	} //doTracking
}
