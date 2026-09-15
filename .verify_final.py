import re

path = 'app/src/main/java/com/example/sayvis/trading/LitStrategyEngine.kt'
lines = open(path, encoding='utf-8').read().splitlines()
B, Q = chr(92), chr(34)

def kt_decode_strict(lit):
    out, i = [], 0
    while i < len(lit):
        c = lit[i]
        if c == B and i + 1 < len(lit):
            n = lit[i + 1]
            if n == 'u':
                out.append(chr(int(lit[i+2:i+6], 16))); i += 6; continue
            out.append({'n': '\n', 't': '\t', B: B, Q: Q}.get(n, n)); i += 2; continue
        if c == Q:
            return ''.join(out), i
        out.append(c); i += 1
    raise ValueError('never terminates')

def literal_of(ln, terminator='.findAll(block)'):
    line = lines[ln - 1].strip()
    m = re.search(r'Regex\("', line)
    start = m.end() - 1
    val, endq = kt_decode_strict(line[start + 1:])
    after = line[start + 1 + endq + 1:]
    assert after.startswith(terminator), f'line {ln}: string closes before {terminator!r} but found {after!r}'
    return val

terminators = {62: ').find(raw)', 66: ').find(raw)', 68: ').findAll(block)', 74: ').find(raw)', 76: '.findAll(block)'}
pats = {ln: literal_of(ln, t) for ln, t in terminators.items()}
for ln, p in pats.items():
    print(ln, repr(p))

tojson = '{"atrFactor":1.2,"rsiHigh":72.0,"rsiLow":28.0,"targets":[3.0,5.0,8.0],"repos":["alpha/scalper"]}'
assert re.compile(pats[62].replace('$field', 'atrFactor')).search(tojson).group(1) == '1.2'
blk = re.compile(pats[66].replace('$field', 'targets')).search(tojson).group(1)
assert blk == '3.0,5.0,8.0'
assert [float(x) for x in re.findall(pats[68], blk)] == [3.0, 5.0, 8.0]
rblk = re.compile(pats[74].replace('$field', 'repos')).search(tojson).group(1)
assert rblk == '"alpha/scalper"'
assert [mm.group(1) for mm in re.finditer(pats[76], rblk)] == ['alpha/scalper']
esc = '"alpha\\"x\\\\y"'
assert [mm.group(1) for mm in re.finditer(pats[76], esc)] == ['alpha"x\\y']
assert re.compile(pats[66].replace('$field', 'targets')).search('not-json') is None
print('ALL FIVE PATTERNS: KOTLIN-SAFE QUOTING + FULL ROUND-TRIP SEMANTICS PASS')
