#!/usr/bin/env bash
# 汇总压测结果：整体吞吐 / 延迟分位 / 错误率 + 分接口 + 容器资源峰值。
#
# 用法：
#   bash report.sh <jtl前缀> [档位列表]
#   例：bash report.sh ramp "1 5 10 25 50 100 200"
#
# 读取同目录下的 <前缀>-<并发>.jtl 与 stats-<并发>.csv（后者由 run-ramp.sh 产出）。
set -u

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR" || exit 1
PFX=${1:-ramp}
LEVELS=${2:-"1 5 10 25 50 100 200 300 500"}

# 从 jtl 取分位（$1=文件 $2=百分位）
pct() {
  awk -F, 'NR>1{print $2}' "$1" 2>/dev/null | sort -n | \
  awk -v p="$2" '{a[NR]=$1} END{
    if(NR==0){print "-"; exit}
    i=int(NR*p/100+0.9999); if(i<1)i=1; if(i>NR)i=NR; print a[i]
  }'
}

echo "==================== 总览（前缀 $PFX）===================="
printf "%-6s %9s %9s %8s %7s %7s %7s %7s %7s %9s\n" \
       "并发" "样本数" "RPS" "avg(ms)" "p50" "p90" "p95" "p99" "max" "错误率"
for N in $LEVELS; do
  f="${PFX}-${N}.jtl"
  [ -f "$f" ] || continue
  read -r n span err mx sum < <(awk -F, '
    NR>1 { n++; sum+=$2+0; if($2+0>mx) mx=$2+0; if($8!="true") err++; if(t0==0)t0=$1; t1=$1 }
    END { printf "%d %.3f %d %d %.1f\n", n, (t1-t0)/1000.0, err, mx, sum }' "$f")
  awk -v N="$N" -v n="$n" -v span="$span" -v err="$err" -v mx="$mx" -v sum="$sum" \
      -v p50="$(pct "$f" 50)" -v p90="$(pct "$f" 90)" -v p95="$(pct "$f" 95)" -v p99="$(pct "$f" 99)" \
      'BEGIN{ if(span<=0)span=1; printf "%-6s %9d %9.1f %8.1f %7s %7s %7s %7s %7s %8.2f%%\n",
         N, n, n/span, sum/n, p50, p90, p95, p99, mx, err*100.0/n }'
done

echo
echo "============== 分接口（取最大档）=============="
LASTN=""
for N in 500 300 200 100 50 25 10 5 1; do
  [ -f "${PFX}-${N}.jtl" ] && { LASTN=$N; break; }
done
if [ -n "$LASTN" ]; then
  LAST="${PFX}-${LASTN}.jtl"
  echo "档位: ${LASTN} 并发"
  printf "%-45s %8s %8s %8s %8s %7s\n" "接口" "样本数" "avg" "p95" "p99" "错误"
  awk -F, 'NR>1{print $3}' "$LAST" | sort -u | while read -r lbl; do
    tmp=$(mktemp)
    awk -F, -v L="$lbl" 'NR>1 && $3==L{print $2}' "$LAST" > "$tmp"
    cnt=$(wc -l < "$tmp")
    err=$(awk -F, -v L="$lbl" 'NR>1 && $3==L && $8!="true"' "$LAST" | wc -l)
    avg=$(awk '{s+=$1} END{printf "%.1f", NR?s/NR:0}' "$tmp")
    s95=$(sort -n "$tmp" | awk '{a[NR]=$1} END{i=int(NR*.95+.9999); if(i<1)i=1; if(i>NR)i=NR; print a[i]}')
    s99=$(sort -n "$tmp" | awk '{a[NR]=$1} END{i=int(NR*.99+.9999); if(i<1)i=1; if(i>NR)i=NR; print a[i]}')
    rm -f "$tmp"
    printf "%-45s %8s %8s %8s %8s %7s\n" "$lbl" "$cnt" "$avg" "$s95" "$s99" "$err"
  done
fi

echo
echo "============== 容器峰值（采样周期 5s）=============="
for N in $LEVELS; do
  f="stats-${N}.csv"
  [ -f "$f" ] || continue
  awk -F, -v N="$N" '
    {
      cpu=$3; gsub(/%/,"",cpu); cpu+=0
      if(cpu>maxc[$2]) maxc[$2]=cpu
      split($4, m, " / "); v=m[1]
      if (v ~ /GiB/)      { gsub(/GiB/,"",v); v=v*1024 }
      else if (v ~ /MiB/) { gsub(/MiB/,"",v) }
      else if (v ~ /KiB/) { gsub(/KiB/,"",v); v=v/1024 }
      else                { gsub(/[A-Za-z]/,"",v); v=v/1048576 }
      v+=0
      if(v>maxm[$2]) maxm[$2]=v
    }
    END {
      for (c in maxc) printf "%-6s %-20s CPU %7.1f%%   Mem %6.0f MiB\n", N, c, maxc[c], maxm[c]
    }' "$f" | sort -k1,1n -k2,2
done
