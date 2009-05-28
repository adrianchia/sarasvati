/*
    This file is part of Sarasvati.

    Sarasvati is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    Sarasvati is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with Sarasvati.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2008 Paul Lorenz
*/

package com.googlecode.sarasvati.mem;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.googlecode.sarasvati.ArcToken;
import com.googlecode.sarasvati.Engine;
import com.googlecode.sarasvati.Env;
import com.googlecode.sarasvati.ExecutionType;
import com.googlecode.sarasvati.GraphProcess;
import com.googlecode.sarasvati.GuardAction;
import com.googlecode.sarasvati.Node;
import com.googlecode.sarasvati.NodeToken;
import com.googlecode.sarasvati.impl.MapEnv;
import com.googlecode.sarasvati.impl.NestedEnv;
import com.googlecode.sarasvati.visitor.TokenVisitor;

public class MemNodeToken implements NodeToken
{
  protected long id;
  protected Node node;
  protected GraphProcess process;
  protected GuardAction guardAction;
  protected List<ArcToken> parentTokens;
  protected List<ArcToken> childTokens;
  protected Date createDate;
  protected Date completeDate;
  protected ExecutionType executionType;

  protected Map<String, String> attributes = new HashMap<String, String>();

  protected Env env = new MapEnv();
  protected Env fullEnv = null;

  public MemNodeToken (long id, Node node, GraphProcess process, ExecutionType executionType, List<ArcToken> parentTokens)
  {
    this.id = id;
    this.node = node;
    this.process = process;
    this.parentTokens = parentTokens;
    this.executionType = executionType;
    this.childTokens = new LinkedList<ArcToken>();
    this.createDate = new Date();
  }

  @Override
  public Long getId ()
  {
    return id;
  }

  @Override
  public Node getNode ()
  {
    return node;
  }

  @Override
  public GraphProcess getProcess ()
  {
    return process;
  }

  @Override
  public GuardAction getGuardAction ()
  {
    return guardAction;
  }

  @Override
  public List<ArcToken> getParentTokens()
  {
    return parentTokens;
  }

  @Override
  public List<ArcToken> getChildTokens()
  {
    return childTokens;
  }

  @Override
  public Date getCreateDate()
  {
    return createDate;
  }

  @Override
  public void recordGuardAction (Engine engine, GuardAction action)
  {
    this.guardAction = action;
  }

  @Override
  public boolean isComplete ()
  {
    return completeDate != null;
  }

  @Override
  public void markComplete (Engine engine)
  {
    completeDate = new Date();
  }

  @Override
  public Date getCompleteDate ()
  {
    return completeDate;
  }

  @Override
  public void accept (TokenVisitor visitor)
  {
    visitor.visit( this );
  }

  @Override
  public Env getFullEnv()
  {
    if ( fullEnv == null )
    {
      fullEnv = new NestedEnv( env, process.getEnv() );
    }
    return fullEnv;
  }

  @Override
  public Env getEnv()
  {
    return env;
  }

  @Override
  public ExecutionType getExecutionType ()
  {
    return executionType;
  }

  @Override
  public void markBacktracked (Engine engine)
  {
    executionType = executionType.getCorrespondingBacktracked( isComplete() );
  }

  @Override
  public String toString()
  {
    return "[MemNodeToken node=" + (node == null ? null : node.getName()) + " execType=" + executionType + " hashCode=" + hashCode() + "]";
  }
}