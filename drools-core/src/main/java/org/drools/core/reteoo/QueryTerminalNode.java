/*
 * Copyright 2005 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.core.reteoo;

import org.drools.core.common.InternalFactHandle;
import org.drools.core.common.InternalWorkingMemory;
import org.drools.core.common.UpdateContext;
import org.drools.core.reteoo.builder.BuildContext;
import org.drools.core.rule.Declaration;
import org.drools.core.rule.GroupElement;
import org.drools.core.rule.Query;
import org.drools.core.rule.Rule;
import org.drools.core.spi.PropagationContext;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;

/**
 * Leaf Rete-OO node responsible for enacting <code>Action</code> s on a
 * matched <code>Rule</code>.
 *
 * @see org.kie.api.definition.rule.Rule
 */
public class QueryTerminalNode extends AbstractTerminalNode implements LeftTupleSinkNode {

    // ------------------------------------------------------------
    // Instance members
    // ------------------------------------------------------------

    private static final long serialVersionUID = 510l;

    public static final short type             = 8;

    /** The rule to invoke upon match. */
    protected Query             query;
    private GroupElement      subrule;
    private int               subruleIndex;    
    private Declaration[]     declarations;

    private LeftTupleSinkNode previousTupleSinkNode;
    private LeftTupleSinkNode nextTupleSinkNode;
    
    private transient ObjectTypeNode.Id leftInputOtnId;

    // ------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------
    public QueryTerminalNode() {
    }

    /**
     * Constructor
     *
     * @param id node ID
     * @param source the tuple source for this node
     * @param rule the rule this node belongs to
     * @param subrule the subrule this node belongs to
     * @param context the current build context
     */
    public QueryTerminalNode(final int id,
                             final LeftTupleSource source,
                             final Rule rule,
                             final GroupElement subrule,
                             final int subruleIndex,                              
                             final BuildContext context) {
        super( id,
               context.getPartitionId(),
               context.getRuleBase().getConfiguration().isMultithreadEvaluation(),
               source );
        this.query = (Query) rule;
        this.subrule = subrule;
        this.subruleIndex = subruleIndex;
        
        initDeclaredMask(context);        
        initInferredMask();        
    }

    // ------------------------------------------------------------
    // Instance methods
    // ------------------------------------------------------------
    public void readExternal(ObjectInput in) throws IOException,
                                            ClassNotFoundException {
        super.readExternal( in );
        query = (Query) in.readObject();
        subrule = (GroupElement) in.readObject();
        subruleIndex = in.readInt();        
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal( out );
        out.writeObject( query );
        out.writeObject( subrule );
        out.writeInt(subruleIndex);
    }

    public Query getQuery() {
        return query;
    }

    public Rule getRule() {
        return this.query;
    }



    public String toString() {
        return "[QueryTerminalNode(" + this.getId() + "): query=" + this.query.getName() + "]";
    }


    public void networkUpdated(UpdateContext updateContext) {
        getLeftTupleSource().networkUpdated(updateContext);
    }

    public boolean isInUse() {
        return false;
    }

    public void updateNewNode(final InternalWorkingMemory workingMemory,
                              final PropagationContext context) {
        // There are no child nodes to update, do nothing.
    }

    public boolean isLeftTupleMemoryEnabled() {
        return false;
    }

    public void setLeftTupleMemoryEnabled(boolean tupleMemoryEnabled) {
        // do nothing, this can only ever be false
    }

    /**
     * @return the subrule
     */
    public GroupElement getSubRule() {
        return this.subrule;
    }
    
    @Override
    public boolean isFireDirect() {
        return false;
    }    
    
    public Declaration[] getDeclarations() {     
        if ( declarations == null ) {
            declarations = new Declaration[ query.getParameters().length ];
            Map<String, Declaration> declMap = subrule.getOuterDeclarations();
            int i = 0;
            for ( Declaration declr : query.getParameters() ) {
                declarations[i++] =  declMap.get( declr.getIdentifier() );
            }
        }
        return declarations;
    }
    
    public int getSubruleIndex() {
        return this.subruleIndex;
    }

    /**
     * Returns the previous node
     * @return
     *      The previous TupleSinkNode
     */
    public LeftTupleSinkNode getPreviousLeftTupleSinkNode() {
        return this.previousTupleSinkNode;
    }

    /**
     * Sets the previous node
     * @param previous
     *      The previous TupleSinkNode
     */
    public void setPreviousLeftTupleSinkNode(final LeftTupleSinkNode previous) {
        this.previousTupleSinkNode = previous;
    }

    /**
     * Returns the next node
     * @return
     *      The next TupleSinkNode
     */
    public LeftTupleSinkNode getNextLeftTupleSinkNode() {
        return this.nextTupleSinkNode;
    }

    /**
     * Sets the next node
     * @param next
     *      The next TupleSinkNode
     */
    public void setNextLeftTupleSinkNode(final LeftTupleSinkNode next) {
        this.nextTupleSinkNode = next;
    }

    public short getType() {
        return NodeTypeEnums.QueryTerminalNode;
    }

    public LeftTuple createLeftTuple(InternalFactHandle factHandle,
                                     LeftTupleSink sink,
                                     boolean leftTupleMemoryEnabled) {
        return new RuleTerminalNodeLeftTuple(factHandle, sink, leftTupleMemoryEnabled );
    }

    public LeftTuple createLeftTuple(final InternalFactHandle factHandle,
                                     final LeftTuple leftTuple,
                                     final LeftTupleSink sink) {
        return new RuleTerminalNodeLeftTuple(factHandle,leftTuple, sink );
    }

    public LeftTuple createLeftTuple(LeftTuple leftTuple,
                                     LeftTupleSink sink,
                                     PropagationContext pctx,
                                     boolean leftTupleMemoryEnabled) {
        return new RuleTerminalNodeLeftTuple(leftTuple,sink, pctx, leftTupleMemoryEnabled );
    }

    public LeftTuple createLeftTuple(LeftTuple leftTuple,
                                     RightTuple rightTuple,
                                     LeftTupleSink sink) {
        return new RuleTerminalNodeLeftTuple(leftTuple, rightTuple, sink );
    }   
    
    public LeftTuple createLeftTuple(LeftTuple leftTuple,
                                     RightTuple rightTuple,
                                     LeftTuple currentLeftChild,
                                     LeftTuple currentRightChild,
                                     LeftTupleSink sink,
                                     boolean leftTupleMemoryEnabled) {
        return new RuleTerminalNodeLeftTuple(leftTuple, rightTuple, currentLeftChild, currentRightChild, sink, leftTupleMemoryEnabled );        
    }    
    
    
    public ObjectTypeNode.Id getLeftInputOtnId() {
        return leftInputOtnId;
    }

    public void setLeftInputOtnId(ObjectTypeNode.Id leftInputOtnId) {
        this.leftInputOtnId = leftInputOtnId;
    }

    public LeftTuple createPeer(LeftTuple original) {
        return null;
    }

    @Override
    public Declaration[] getSalienceDeclarations() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Declaration[][] getTimerDeclarations() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void assertLeftTuple(LeftTuple leftTuple, PropagationContext context, InternalWorkingMemory workingMemory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void retractLeftTuple(LeftTuple leftTuple, PropagationContext context, InternalWorkingMemory workingMemory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void modifyLeftTuple(LeftTuple leftTuple, PropagationContext context, InternalWorkingMemory workingMemory) {
        throw new UnsupportedOperationException();
    }

    public void attach( BuildContext context ) {
        getLeftTupleSource().addTupleSink( this, context );
    }
}
