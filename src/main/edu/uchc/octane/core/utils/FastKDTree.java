package edu.uchc.octane.core.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntUnaryOperator;

/* fast KDTree implementation desgined for very large number of points */

public class FastKDTree {

	int [] pointers;
	int [] left;
	int [] right;
	HDataCollection data;
	int root;

	//search
	int refIdx;
	ArrayList<Integer> results;

	public FastKDTree(HDataCollection data) {
		pointers = new int[data.size()];
		left = new int[data.size()];
		right = new int[data.size()];
		this.data = data;
		buildTree();
	}

	public int getRoot() {
		return root;
	}

	public int getLeft(int p) {
		return left[p];
	}

	public int getRight(int p) {
		return right[p];
	}

	private void buildTree() {
		Arrays.setAll(pointers, IntUnaryOperator.identity());
		root = generate(0, 0, data.size() - 1);
	}

//	private void buildUnbalancedTree() {
//		Arrays.setAll(pointers, IntUnaryOperator.identity());
//		Arrays.fill(left, -1);
//		Arrays.fill(right, -1);
//		//shuffle
//		Random rnd = new Random();
//		for (int i = pointers.length - 1; i > 0; i--  ) {
//			int index = rnd.nextInt(i + 1);
//			swap(i, index);
//		}
//
//
//		for (int i = 1; i < pointers.length; i ++) {
//			refIdx = i;
//			insert(0, 0);
//		}
//
//		root = 0;
//	}

//	private void insert(int treeIdx, int d) {
//
//		data.selectDimension(d);
//
//		if (++d >= data.getDimension()) {d = 0;}
//
//		if (getData(treeIdx) < getData(refIdx)) {
//			if (getRight(treeIdx) != -1) {
//				insert(getRight(treeIdx), d);
//			} else {
//				right[treeIdx] = refIdx;
//				return;
//			}
//        } else {
//        	if (getLeft(treeIdx) != -1 ) {
//        		insert(getLeft(treeIdx), d);
//        	} else {
//        		left[treeIdx] = refIdx;
//        		return;
//        	}
//        }
//	}

	private int generate (int d, int left, int right) {

		// Handle the easy cases first
		if (right < left) { return -1; }
		if (right == left) {
			this.left[left] = -1;
			this.right[left] = -1;
			return left;
		}

		int m = 1+(right-left)/2;

		data.selectDimension(d);
		partitionOnRank(left, right, m);

		// update to the next dimension
		if (++d >= data.getDimension()) { d = 0; }

		// recursively compute left and right sub-trees, which translate into
		// 'below' and 'above' for n-dimensions.
		this.left[left + m - 1] = generate (d, left, left+m-2);
		this.right[left + m - 1] = generate (d, left+m, right);
		return left + m - 1;
	}

	private int selectPivotIndex (int left, int right) {
		int midIndex = (left+right)/2;

		int lowIndex = left;

		if (getData(lowIndex) >= getData(midIndex)) {
			lowIndex = midIndex;
			midIndex = left;
		}

		// when we get here, we know that ar[lowIndex] < ar[midIndex]

		// select middle of [low,mid] and ar[right]
		if (getData(right) <= getData(lowIndex)) {
			return lowIndex;  // right .. low .. mid     so we return
		} else if (getData(right) > getData(midIndex) ) {
			return midIndex;  // low .. mid .. right
		}

		return right; // why not
	}

	private int partitionOnRank (int left, int right, int rank) {
		do {
			int idx = selectPivotIndex (left, right);

			int pivotIndex = partitionOnElement (left, right, idx);
			if (left+rank-1 == pivotIndex) {
				return pivotIndex;
			}

			// continue the loop, narrowing the range as appropriate.
			if (left+rank-1 < pivotIndex) {
				// we are within the left-hand side of the pivot. k can stay the same
				right = pivotIndex - 1;
			} else {
				// we are within the right-hand side of the pivot. k must decrease by
				// the size being removed.
				rank -= (pivotIndex-left+1);
				left = pivotIndex + 1;
			}
		} while (true);
	}

	private int partitionOnElement (int left, int right, int indexOfElement) {
		double pivotValue = getData(indexOfElement);
		swap(right, indexOfElement);

		int store = left;
		for (int idx = left; idx < right; idx++) {
			if (getData(idx) <= pivotValue) {
				swap(idx, store);
				store++;
			}
		}

		// move pivot to its final place
		swap(right, store);
		return store;
	}

	private void swap(int a, int b) {
		int tmp = pointers[a];
		pointers[a] = pointers[b];
		pointers[b] = tmp;
	}

	private double getData(int idx) {
		return data.get(pointers[idx]);
	}

	public List<Integer> radiusSearch(int ref, final double radius) {

		results = new ArrayList<Integer> ();
		double sqRadius = radius * radius;
		refIdx = ref;
		radiusSearch(getRoot(), 0, sqRadius);

		return results;
	}

	protected void radiusSearch(int current, int d, final double sqRadius )
	{
		// consider the current node
		final double sqDistance = sqDistance(pointers[current], refIdx);
		if ( sqDistance <= sqRadius )
		{
			results.add( new Integer(current) );
		}
		data.selectDimension(d);
		final double axisDiff = data.get(refIdx) - getData(current);
		final double axisSqDistance = axisDiff * axisDiff;

		// search the near branch
		int nearChild = axisDiff < 0 ? getLeft(current) : getRight(current);
		int awayChild = axisDiff < 0 ? getRight(current): getLeft(current);

		if (++d >= data.getDimension()) { d= 0 ;}

		if ( nearChild != -1 )
			radiusSearch( nearChild, d, sqRadius );

		// search the away branch - maybe
		if ( ( axisSqDistance <= sqRadius ) && ( awayChild != -1 ) )
			radiusSearch(awayChild, d, sqRadius );
	}

	private double sqDistance(int a, int b) {
		double d = 0;
		for (int i = 0; i < data.getDimension(); i ++ ) {
			double d1 = (data.get(a, i) - data.get(b, i));
			d += d1 * d1;
		}

		return d;
	}

	public List<Integer> dumbSearch(int ref, final double radius) {
		results = new ArrayList<Integer> ();
		double sqRadius = radius * radius;
		for ( int i = 0; i < data.size(); i ++) {
			if ( sqDistance(ref, i) < sqRadius ) {
				results.add(new Integer(i));
			}
		}
		return results;
	}
}
