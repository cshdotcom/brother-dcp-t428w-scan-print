#!/usr/bin/env python3
"""
Strip MethodParameters attributes from .class files.

JDK 21 javac 对匿名类构造器强制生成 MethodParameters 属性，且合成参数
（如 this$0）的 name_index=0（<no name>）。build-tools 33/34 的 d8 在把
该属性转成 dex 字符串时对 null 名字直接 NPE（"String.length()" 崩溃）。

参数名仅是反射元数据（java.lang.reflect.Parameter），本 APP 不使用，
剥离后对运行时行为零影响。

用法： python3 strip-methodparams.py <class 文件或目录> ...
"""
import os
import struct
import sys


def parse_cp(data):
    """返回 (pool, next_pos)。pool: index -> name 字符串（仅 Utf8）"""
    cp_count = struct.unpack('>H', data[8:10])[0]
    i = 1
    pool = {}
    pos = 10
    while i < cp_count:
        tag = data[pos]
        if tag == 1:
            ln = struct.unpack('>H', data[pos + 1:pos + 3])[0]
            pool[i] = data[pos + 3:pos + 3 + ln].decode('utf-8', 'replace')
            pos += 3 + ln
        elif tag in (5, 6):
            pos += 9
            i += 1  # long/double 占两个槽位
        elif tag in (7, 8, 16, 19, 20):
            pos += 3
        elif tag == 15:
            pos += 4
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            pos += 5
        else:
            raise ValueError(f'unknown cp tag {tag} at pos {pos}')
        i += 1
    return pool, pos


def strip_class(data):
    pool, pos = parse_cp(data)
    pos += 6  # access_flags, this_class, super_class

    ifc = struct.unpack('>H', data[pos:pos + 2])[0]
    pos += 2 + 2 * ifc

    out = bytearray()
    out += data[:pos]  # magic + 版本 + 常量池 + 访问标志 + this/super + 接口表

    def rewrite_member_table(pos):
        """pos 指向成员数量。逐成员处理：
        [access u2][name u2][desc u2][attrs u2][attr...] × n
        每个成员的属性表剔除 MethodParameters（attrs_count 相应修正）。
        返回 (新字节, 下一位置)。"""
        n = struct.unpack('>H', data[pos:pos + 2])[0]
        p = pos + 2
        buf = bytearray()
        buf += struct.pack('>H', n)
        for _ in range(n):
            header = bytes(data[p:p + 8])  # access/name/desc/attrs_count
            na = struct.unpack('>H', header[6:8])[0]
            p += 8
            keep = 0
            body = bytearray()
            for _ in range(na):
                nidx, ln = struct.unpack('>HI', data[p:p + 6])
                if pool.get(nidx) != 'MethodParameters':
                    body += data[p:p + 6 + ln]
                    keep += 1
                p += 6 + ln
            # 头部前 6 字节 + 修正后的 attrs_count + 属性体
            buf += header[:6] + struct.pack('>H', keep) + body
        return bytes(buf), p

    # fields
    fbytes, pos = rewrite_member_table(pos)
    out += fbytes
    # methods
    mbytes, pos = rewrite_member_table(pos)
    out += mbytes

    # class 级属性表（原样保留）
    na = struct.unpack('>H', data[pos:pos + 2])[0]
    out += struct.pack('>H', na)
    p = pos + 2
    for _ in range(na):
        ln = struct.unpack('>I', data[p + 2:p + 6])[0]
        out += data[p:p + 6 + ln]
        p += 6 + ln

    out += data[p:]
    return bytes(out)


def main():
    targets = []
    for arg in sys.argv[1:]:
        if os.path.isdir(arg):
            for root, _, files in os.walk(arg):
                for fn in files:
                    if fn.endswith('.class'):
                        targets.append(os.path.join(root, fn))
        elif os.path.isfile(arg) and arg.endswith('.class'):
            targets.append(arg)

    stripped = 0
    for path in targets:
        with open(path, 'rb') as f:
            data = f.read()
        try:
            new = strip_class(data)
        except Exception as e:
            print(f'  SKIP {path}: {e}')
            continue
        if new != data:
            with open(path, 'wb') as f:
                f.write(new)
            stripped += 1
    print(f'strip-methodparams: {stripped}/{len(targets)} class files modified')


if __name__ == '__main__':
    main()
