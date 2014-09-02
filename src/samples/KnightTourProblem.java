/*
 * Copyright (c) 1999-2012, Ecole des Mines de Nantes
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Ecole des Mines de Nantes nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package samples;

import org.kohsuke.args4j.Option;
import samples.input.HCP_Utils;
import solver.Solver;
import solver.cstrs.GraphConstraintFactory;
import solver.search.loop.monitors.SearchMonitorFactory;
import solver.search.GraphStrategyFactory;
import solver.search.strategy.ArcStrategy;
import solver.search.strategy.GraphStrategy;
import solver.variables.UndirectedGraphVar;
import util.objects.setDataStructures.ISet;
import util.objects.setDataStructures.SetFactory;
import util.objects.setDataStructures.SetType;

/**
 * Solves the Knight's Tour Problem
 * <p/>
 * Uses graph variables (light data structure)
 * Scales up to 200x200 in ten seconds
 * requires -Xms1048m -Xmx2048m for memory allocation
 *
 *
 * @author Jean-Guillaume Fages
 * @since Oct. 2012
 */
public class KnightTourProblem extends AbstractProblem {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    @Option(name = "-tl", usage = "time limit.", required = false)
    private long limit = 20000;
    @Option(name = "-bl", usage = "Board length.", required = false)
    private int boardLength = 120;
    @Option(name = "-open", usage = "Open tour (path instead of cycle).", required = false)
    private boolean closedTour = true;

    private UndirectedGraphVar graph;

    //***********************************************************************************
    // METHODS
    //***********************************************************************************

    public static void main(String[] args) {
        new KnightTourProblem().execute(args);
    }

    @Override
    public void createSolver() {
		level = Level.SILENT;
        solver = new Solver("solving the knight's tour problem with graph variables");
    }

    @Override
    public void buildModel() {
        boolean[][] matrix;
        if (closedTour) {
            matrix = HCP_Utils.generateKingTourInstance(boardLength);
        } else {
            matrix = HCP_Utils.generateOpenKingTourInstance(boardLength);
        }
        // variables
		int n = matrix.length;
		SetFactory.RECYCLE = false; //(optim)
		graph = new UndirectedGraphVar("G", solver, n, SetType.LINKED_LIST, SetType.LINKED_LIST, true);
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (matrix[i][j]) {
                    graph.getEnvelopGraph().addEdge(i, j);
                }
            }
        }
        // constraints
        solver.post(GraphConstraintFactory.hamiltonianCycle(graph));
    }

    @Override
    public void configureSearch() {
        // basically branch on sparse areas of the graph
        solver.set(GraphStrategyFactory.graphStrategy(graph, null, new MinNeigh(graph), GraphStrategy.NodeArcPriority.ARCS));
        SearchMonitorFactory.limitTime(solver, limit);
		SearchMonitorFactory.log(solver,false,false);
    }

    @Override
    public void solve() {
        solver.findSolution();
    }

    @Override
    public void prettyOut() {}

    //***********************************************************************************
    // HEURISTICS
    //***********************************************************************************

    private static class MinNeigh extends ArcStrategy<UndirectedGraphVar> {
        int n;

        public MinNeigh(UndirectedGraphVar graphVar) {
            super(graphVar);
            n = graphVar.getEnvelopGraph().getNbNodes();
        }

        @Override
        public boolean computeNextArc() {
            ISet suc;
            int size = n + 1;
            int sizi;
			from = -1;
            for (int i = 0; i < n; i++) {
                sizi = g.getEnvelopGraph().getNeighborsOf(i).getSize() - g.getKernelGraph().getNeighborsOf(i).getSize();
                if (sizi < size && sizi > 0) {
                    from = i;
                    size = sizi;
                }
            }
            if (from == -1) {
                return false;
            }
            suc = g.getEnvelopGraph().getNeighborsOf(from);
            for (int j = suc.getFirstElement(); j >= 0; j = suc.getNextElement()) {
                if (!g.getKernelGraph().edgeExists(from, j)) {
					to = j;
					return true;
                }
            }
			throw new UnsupportedOperationException();
        }
    }
}