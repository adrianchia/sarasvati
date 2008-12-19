package com.googlecode.sarasvati.visitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.googlecode.sarasvati.ArcToken;
import com.googlecode.sarasvati.Engine;
import com.googlecode.sarasvati.ExecutionType;
import com.googlecode.sarasvati.NodeToken;
import com.googlecode.sarasvati.WorkflowException;

public class BacktrackTokenVisitor implements TokenVisitor
{
  protected Engine engine;
  protected NodeToken destinationToken;

  protected LinkedList<NodeToken> queue = new LinkedList<NodeToken>();
  protected Set<ArcToken> arcTokenLeaves = new HashSet<ArcToken>();

  protected Set<NodeToken> visited = new HashSet<NodeToken>();
  protected Map<ArcToken,ArcToken> arcTokenMap = new HashMap<ArcToken, ArcToken>();

  public BacktrackTokenVisitor (Engine engine, NodeToken destinationToken)
  {
    this.engine = engine;
    this.destinationToken = destinationToken;
  }

  @Override
  public void visit (NodeToken token)
  {
    if ( !token.getNode().isBacktrackable( token ) )
    {
      throw new WorkflowException( "Can not backtrack node name: " +
                                   token.getNode().getName()  +
                                   "id: " + token.getNode().getId() );
    }

    if ( token.getChildTokens().isEmpty() )
    {
      queue.add( token );
    }
    visited.add( token );
  }

  @Override
  public void visit (ArcToken token)
  {
    if ( token.getChildToken() == null )
    {
      arcTokenLeaves.add( token );
    }
  }

  private void backtrackLeafArcTokens ()
  {
    for ( ArcToken token : arcTokenLeaves )
    {
      arcTokenMap.put( token, token );
      token.markBacktracked( engine );
      queue.add( token.getParentToken() );
      token.getProcess().removeActiveArcToken( token );
    }
  }

  public NodeToken backtrack ()
  {
    backtrackLeafArcTokens();

    NodeToken resultToken = null;

    while ( !queue.isEmpty() )
    {
      NodeToken token = queue.removeFirst();
      boolean isDestination = token == destinationToken;

      if ( isDestination )
      {
        resultToken = backtrackCompletedToken( token, ExecutionType.Forward );
      }
      else
      {
        NodeToken backtrackToken = backtrackToken( token );
        backtrackToken.markBacktracked( engine );
        backtrackToken.markComplete( engine );
      }
    }

    return resultToken;
  }

  private NodeToken backtrackCompletedToken (NodeToken token, ExecutionType executionType)
  {
    token.markBacktracked( engine );

    List<ArcToken> parents = new ArrayList<ArcToken>( token.getChildTokens().size() );
    for ( ArcToken childToken : token.getChildTokens() )
    {
      parents.add( arcTokenMap.get( childToken ) );
    }

    NodeToken backtrackToken =
      engine.getFactory().newNodeToken( token.getProcess(),
                                                 token.getNode(),
                                                 executionType,
                                                 parents,
                                                 token );

    for ( ArcToken parent : parents )
    {
      parent.markComplete( engine, backtrackToken );
    }

    return backtrackToken;
  }

  private NodeToken backtrackToken (NodeToken token)
  {
    NodeToken backtrackToken = token;

    if ( !token.isComplete() )
    {
      token.markComplete( engine );
      token.markBacktracked( engine );
      token.getProcess().removeActiveNodeToken( token );
    }
    else if ( !token.getExecutionType().isBacktracked() )
    {
      backtrackToken = backtrackCompletedToken( token, ExecutionType.Backward );
    }

    for ( ArcToken parent : getParents( token ) )
    {
      boolean backtrackParent = visited.contains( parent.getParentToken() );

      parent.markBacktracked( engine );
      ArcToken backtrackArcToken =
        engine.getFactory().newArcToken( token.getProcess(),
                                         parent.getArc(),
                                         backtrackParent ? ExecutionType.Backward : ExecutionType.UTurn,
                                         backtrackToken );

      backtrackToken.getChildTokens().add( backtrackArcToken );

      arcTokenMap.put( parent, backtrackArcToken );

      if ( backtrackParent )
      {
        backtrackArcToken.markBacktracked( engine );
        queue.add( parent.getParentToken() );
        backtrackArcToken.markProcessed( engine );
      }
      else
      {
        token.getProcess().enqueueArcTokenForExecution( backtrackArcToken );
      }
    }

    return backtrackToken;
  }

  private List<ArcToken> getParents (NodeToken token)
  {
    while ( true )
    {
      List<ArcToken> parents = token.getParentTokens();
      if ( parents.isEmpty() ||
           !parents.get( 0 ).getExecutionType().isBacktracked() )
      {
        return parents;
      }
      token = parents.get( 0 ).getParentToken();
    }
  }
}