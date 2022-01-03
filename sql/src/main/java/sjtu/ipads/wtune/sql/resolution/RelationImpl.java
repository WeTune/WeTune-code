package sjtu.ipads.wtune.sql.resolution;

import sjtu.ipads.wtune.common.utils.Lazy;
import sjtu.ipads.wtune.sql.ast1.SqlNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;

class RelationImpl implements Relation {
  private final SqlNode rootNode;
  private final String qualification;
  private final Lazy<List<Relation>> inputs;
  private List<Attribute> attributes;

  RelationImpl(SqlNode rootNode, String qualification) {
    this.rootNode = rootNode;
    this.qualification = qualification;
    this.inputs = Lazy.mk(ArrayList::new);
    this.attributes = null;
  }

  @Override
  public SqlNode rootNode() {
    return rootNode;
  }

  @Override
  public String qualification() {
    return qualification;
  }

  @Override
  public List<Relation> inputs() {
    return inputs.isInitialized() ? inputs.get() : emptyList();
  }

  @Override
  public List<Attribute> attributes() {
    return attributes;
  }

  @Override
  public Attribute resolveAttribute(String qualification, String name) {
    if (qualification != null && !Objects.equals(qualification, this.qualification)) return null;
    for (Attribute attribute : attributes)
      if (Objects.equals(attribute.name(), name)) {
        return attribute;
      }
    return null;
  }

  @Override
  public String toString() {
    return "Relation{" + rootNode.toString() + "}";
  }

  void addInput(Relation input) {
    inputs.get().add(input);
  }

  void setAttributes(List<Attribute> attrs) {
    this.attributes = attrs;
  }
}