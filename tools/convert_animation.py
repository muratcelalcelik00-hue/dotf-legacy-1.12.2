#!/usr/bin/env python3
"""Convert a GeckoLib 4 (MC 1.20.1) animation.json into the subset GeckoLib 3.0.x
for Minecraft 1.12.2 understands.

GeckoLib 3.0.x (1.12.2) reads keyframes either as a plain [x, y, z] array or as an
object with a "vector" key. Newer Blockbench exports wrap the vector into
{"post": {"vector": [...]}, "lerp_mode": "catmullrom"} which the old parser cannot
read (it would NPE on the missing "vector" key). This script flattens those forms and
drops "lerp_mode" (catmull-rom is approximated with linear interpolation).
"""
import json
import sys


def convert_keyframe(value):
    if isinstance(value, dict):
        if "vector" in value:
            out = {"vector": value["vector"]}
            if "easing" in value:
                out["easing"] = value["easing"]
            if "easingArgs" in value:
                out["easingArgs"] = value["easingArgs"]
            return out
        if "post" in value:
            post = value["post"]
            if isinstance(post, dict):
                post = post.get("vector", post)
            return {"vector": post}
        if "pre" in value:
            pre = value["pre"]
            if isinstance(pre, dict):
                pre = pre.get("vector", pre)
            return {"vector": pre}
    return value


def convert_channel(channel):
    # Channel can be a static vector ([x,y,z] or {"vector": [...]}) or a time->keyframe map
    if isinstance(channel, list):
        return channel
    if isinstance(channel, dict):
        if "vector" in channel or "post" in channel or "pre" in channel:
            return convert_keyframe(channel)
        return {time: convert_keyframe(kf) for time, kf in channel.items()}
    return channel


def main(src, dst):
    with open(src) as f:
        data = json.load(f)
    changed = 0
    for anim in data["animations"].values():
        for bone in anim.get("bones", {}).values():
            for ch_name in ("rotation", "position", "scale"):
                if ch_name in bone:
                    before = json.dumps(bone[ch_name], sort_keys=True)
                    bone[ch_name] = convert_channel(bone[ch_name])
                    if json.dumps(bone[ch_name], sort_keys=True) != before:
                        changed += 1
    data["format_version"] = "1.8.0"
    with open(dst, "w") as f:
        json.dump(data, f, indent="\t")
    print("converted channels:", changed)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
