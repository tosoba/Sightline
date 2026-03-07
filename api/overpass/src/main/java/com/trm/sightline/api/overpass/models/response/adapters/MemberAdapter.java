package com.trm.sightline.api.overpass.models.response.adapters;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.ToJson;
import com.trm.sightline.api.overpass.models.query.statements.base.Type;
import com.trm.sightline.api.overpass.models.response.geometries.members.Member;
import com.trm.sightline.api.overpass.models.response.geometries.members.NodeMember;
import com.trm.sightline.api.overpass.models.response.geometries.members.WayMember;

import java.io.IOException;

public class MemberAdapter {

  private Moshi moshi = new Moshi.Builder().build();
  private JsonAdapter<Member> memberAdapter = moshi.adapter(Member.class);
  private JsonAdapter<MemberJson> jsonAdapter = moshi.adapter(MemberJson.class);
  private JsonAdapter<NodeMember> nodeAdapter = moshi.adapter(NodeMember.class);
  private JsonAdapter<WayMember> wayAdapter = moshi.adapter(WayMember.class);

  @ToJson
  public MemberJson toJson(Member member) throws IOException {
    String string = memberAdapter.toJson(member);
    return jsonAdapter.fromJson(string);
  }

  @FromJson
  public Member fromJson(MemberJson json) throws IOException {
    String userJson = jsonAdapter.toJson(json);
    switch (json.type) {
      case Type.NODE:
        return nodeAdapter.fromJson(userJson);
      case Type.WAY:
        return wayAdapter.fromJson(userJson);
      default:
        throw new IllegalArgumentException(
            String.format("type '%s' is not implemented yet.", json.type));
    }
  }
}
