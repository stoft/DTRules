/**
 * Copyright 2004-2011 DTRules.com, Inc.
 *
 * See http://DTRules.com for updates and documentation for the DTRules Rules Engine
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
 **/

package com.dtrules.mapping;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Paul Snow
 *
 */
public class XMLComment implements XMLNode {

  Object body;
  XMLNode parent;

  @Override
  public int childCount() {
    return 0;
  }

  /* (non-Javadoc)
   * @see com.dtrules.mapping.XMLNode#addChild(com.dtrules.mapping.XMLNode)
   */
  @Override
  public void addChild(XMLNode node) {}

  /* (non-Javadoc)
   * @see com.dtrules.mapping.XMLNode#remove(com.dtrules.mapping.XMLNode)
   */
  @Override
  public void remove(XMLNode node) {}

  public HashMap<String, Object> getAttribs() {
    return null;
  }

  /* (non-Javadoc)
   * @see com.dtrules.mapping.XMLNode#getBody()
   */
  public Object getBody() {
    return body;
  }

  /* (non-Javadoc)
   * @see com.dtrules.mapping.XMLNode#getParent()
   */
  public XMLNode getParent() {
    return parent;
  }

  /* (non-Javadoc)
   * @see com.dtrules.mapping.XMLNode#getTag()
   */
  public String getTag() {
    return null;
  }

  /* (non-Javadoc)
   * @see com.dtrules.mapping.XMLNode#getTags()
   */
  public ArrayList<XMLNode> getTags() {
    return null;
  }

  /* (non-Javadoc)
   * @see com.dtrules.mapping.XMLNode#setBody(java.lang.Object)
   */
  public void setBody(Object body) {
    this.body = body;
  }

  /* (non-Javadoc)
   * @see com.dtrules.mapping.XMLNode#setParent(com.dtrules.mapping.XMLTag)
   */
  public void setParent(XMLTag parent) {
    this.parent = parent;
  }

  /**
   *  A comment cannot have a tag.
   */
  public void setTag(String tag) {}

  /**
   *  A comment cannot have tags
   * @see com.dtrules.mapping.XMLNode#setTags(java.util.ArrayList)
   */
  public void setTags(ArrayList<XMLNode> tags) {}

  public String toString() {
    return "<!--" + body + "-->";
  }

  public Type type() {
    return Type.COMMENT;
  }

  @Override
  public Object getAttrib(String key) {
    return null;
  }

  @Override
  public void setAttrib(String key, Object value) {}

  @Override
  public void clearRef() {}
}
