package com.eharmony.aloha.util.hashing

import com.eharmony.aloha.util.hashing.MurmurHash3Test.tests
import org.junit.Test
import org.junit.Assert._
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

/**
 * This test is designed to fail if the MurmurHash3 implementation is changed.
 */
@RunWith(classOf[BlockJUnit4ClassRunner])
class MurmurHash3Test {
    @Test def testHashing(): Unit = {
        val scalaFails = fails(scala.util.hashing.MurmurHash3.stringHash)
        val alohaFails = fails(MurmurHash3.stringHash)

        if (scalaFails.nonEmpty || alohaFails.nonEmpty) {
            fail(failString(scalaFails, alohaFails))
        }
    }

    private[this] def failString(scalaFails: Seq[(String, Int, Int)], alohaFails: Seq[(String, Int, Int)]): String = {
        "MurmurHash3 values changed:\n" +
        scalaFails.foldLeft("\n\tscala values:")((s, x) => s"$s\n\texpected: ${x._2}, actual: ${x._3}, string: '${x._1}'") +
        alohaFails.foldLeft("\n\n\taloha values:")((s, x) => s"$s\n\texpected: ${x._2}, actual: ${x._3}, string: '${x._1}'")
    }


    private[this] def fails(hash: String => Int) =
        tests.flatMap { case(s, h) =>
            val y = hash(s)
            if (y != h)
                Option((s, h, y))
            else None
        }
}

private object MurmurHash3Test {

    /**
     * These tests were generated in the scala 2.10.2 REPL with the following:
     *
     * {{{
     * val r = new scala.util.Random(0)
     *
     * val ins = (
     *   for {
     *     i <- 1 to 100
     *     s = r.nextInt(100)
     *     // str = Seq.fill(s)(r.nextPrintableChar).mkString.replaceAllLiterally("\\", "\\\\").replaceAllLiterally("\"", "\\\"")
     *     str = Seq.fill(s)(r.nextPrintableChar).mkString
     *   } yield str
     * ).sortWith(_.length < _.length).distinct
     *
     * val outs = ins.map(s => scala.util.hashing.MurmurHash3.stringHash(s))
     *
     * ins.zip(outs).foreach{case(i, o) =>
     *   val iClean = i.replaceAllLiterally("\\", "\\\\").replaceAllLiterally("\"", "\\\"")
     *   println(s""""$iClean" -> $o,""")
     * }
     * }}}
     */
    val tests = Vector (
        "" -> 377927480,
        "yZf" -> -354277552,
        "wi+" -> -1397715261,
        "/\\dsQ1" -> -249963444,
        "^},j9gU" -> 526024369,
        "Dw?w>TAX" -> 513957158,
        "L\\~Lpk$pu" -> -1647065402,
        "12ycS!Lvt`" -> -1670636453,
        "hcf=\"Uo'n5" -> -921384650,
        "3q[~^9!\\gPW" -> -767173678,
        "OTKk;!hc*d0" -> 190176367,
        "Z+8UyaFZ~!e" -> 977004949,
        "4K_;Xx?Nj\"v" -> -1766926774,
        "q|W*Lb?e&m|;*" -> 1278801863,
        "3@s0d7,a\\ol.a" -> -1903121081,
        ">Uv.,{!8\"[q\\,s" -> 1226259594,
        "CiM|_wU/}^{I7\\zT" -> -938156375,
        "+Lvx$W=%jF:\\:-q<I" -> -2036455897,
        "*U'{Et@A<s)*T{r?3" -> 1408917769,
        "FpIX@tm?Y`b).cY'/_," -> 1969250575,
        "ujPNI/prw[aC,xAsctt" -> 564927617,
        "S0ki|$SO^8aqM$ap7>z1F<0" -> 161743514,
        "-@|#:-`lB+1RI=</fIg8Fs<C" -> -2114269323,
        "b^{JM|Kvt.(BqY6E[:{BZcYh" -> 2136996571,
        "gz;jc]dhQ2%7Ji_d2y'*?YA\"b" -> -1359211787,
        "r#$PABpQ<~W~&RP4S\"\"--{>X+y" -> -880463855,
        "D6*yv*;HZ%\\'T)I}x}G6r.8k=S" -> 2044315863,
        "upiu@nPKdr;,PL.1[!:~.sZIPm~" -> -1188119971,
        ")lm-MXcs7bs>cnhbB]27:|DFep;" -> -1554124450,
        "\"\\DMg(wr}Nc24QWK%J#iRGSlb<-B" -> -535086942,
        "_kqtfhfMz:|m3/W%zL1MZ}_D=l`xM" -> -640266030,
        "$poz}\"^>?{gOCM9M7V]u,(+L%4HpEk" -> 1336025516,
        "}|.fx6hhPIG9c,x_\"`S#l0Je83G2zq'" -> 1135713171,
        "6ZrH\\npW5`WJgV{oX?CS)u`yGYU+$0C?" -> 1700887910,
        "gg5z>ToT^@$~{`|S~'?HzY[=UTJ51fX}" -> -65251615,
        "xjMv-6aPO.G?VFwSB7dUYLH;eSdIWWt?" -> 20897566,
        "[b0OreTP2F<s.[168SW[zlShNmb;d*V2W" -> 2133433544,
        "__Zv,dzu2}T-Ljv>CR]6?Ds?{cVg.~gT_T%" -> 1436823132,
        "MKe=)b^_!}6w8a01?\\%&\\&AYkO]G'6m'OlN" -> 339254694,
        "M!eI7U*-s;,nRW3g'Ar/hw]v0+A>Zc\"/WnZ_" -> 1387576663,
        "}\"7PkSnKPEz+yAACDlyU^+uvf+!\\e\\j$d?ix~" -> 897970675,
        "wfSa!|}}i{i/SgEqo\"6{;P3w{vyqsMaW/TPxxP" -> -1100821181,
        "0L~:}l|/wTA')a4Mf()&2i-qUz?}Wek`fj[vBX" -> 1945229429,
        "lJ^~)lGq$!<rZ\"rzKDrWfE9S\\O!+JM,%#XCE8s[" -> -1642213206,
        "+#nS&Wxr8U'KAw6?2\\4VrI=T7rg+Pua/,5`kr#f^R" -> -1376696324,
        "~^twR1=l'`4F^;f\"Ph+.K,Q6yDIVrO'L]@(ud=O'+wp^" -> -470498682,
        "-#im7`\"Hrl/>~C&q}0P1~|Xwj?)i:P0,b6-{I%1XFW9XV" -> 328001450,
        "*QKN@~1N.o@HWTE[L*$6#GDAB0w.YsBMo5$\\V`MBXsI,^" -> -1819136836,
        "&`/-}V.6O#wb$RU~e~\\\"?NIg}.3@W?yIJ9Wn*:=^#4[y=" -> 526041157,
        "I4])0gMrPE|TD`!O\\kHj-k7mF%sT.>fO$XHg&dW}5*kC*N" -> -1306208749,
        "a??~`-~\"x:Z\"ol0Z%uuKG1@qP&fb^?&1E!wcz}qScJ)lOg" -> -1599686130,
        "_XjEHQL-@dNFA9g^5V/1?3uJl[4%rWnF^FvOaD2.;.CNly" -> -1331192724,
        "x?yo$a2\\e0SzS[sk`SC2Q;D7AH<4R-cM:Q^9H@YBJK|eK{jQ" -> 29929697,
        ")_|y;rXS*-z@2v/9;<){kNqTe.((ro*iClIRb4b?&tx]}s;$" -> -861823988,
        "P7/aV)ln=-+H<NgbI-ZY/;0.+b~LZh.-5UtGNcjwoe`VKXm}" -> -1200785812,
        "jh`$:98Sif0K!2\"2@uW<c)r0g2#!BXe[v/\"g%PxsE$c_}tt]2'b" -> -912824994,
        "XC])[J:<cFhi`@AmyQ^/>Bx[%'#J@A2y3`N+~[UhvQdO`a+ZT/5" -> 1960916727,
        "}+bC0.Tx$sl/wtem$mf\\>Qf/1USTmaOj&t/6/X;dfM[:}ClRK2E6G" -> -972672354,
        "1wI%@VM4J7aj+}A|GTDYM\"9p8/q3ziiA1qf(uP]Gp<lH)b(O&I`,1P" -> 1599177774,
        "MK9-YXaf:t?*y1.IxB.&7(1}kW#w&4PhDc!,<G%sIvTR6n[h{36?HmZ" -> -2056222968,
        "SS@F%5i]CB<8$sdr8L)SW(Us_$LhX4@&VKaaE0SfSY,3-QFy+G,!iX#M" -> 1079233243,
        ":iCbQr1}kX$kz0qgR2fE#*:/ka*kC`[~<\\A5zEe%zRD3xp+\\iS:<{6#m" -> -440857644,
        "i.n,fS0x~M8OL0Qmllfs[]l{-}!uqNLkr1?$tv}7]B<0LIdQ3qUupBb!!" -> 365292325,
        "OWg8JShEh!H$\\|L|ee^$}#7D!~0o%V;>S4`I9<Y0kkk{Uj5;2M$&1`-o1" -> 1032092208,
        "-W``vwHHcRC+1YS_N71ab^;-[cO55d[}+nWEd3)FI$=tMU[bwNI;DpyP%u" -> -1264755486,
        "^K!\"7czvV-6W6n;p:eJ,e4'.!Wk,t?8k!%{<P:]=f{7>P}Qxd'I02iu|yAo" -> -912867902,
        "-l\\&fjV|o^dfe|mInv]8y}LIk@Yo&Zr@JU+`X5R7D</[$Eu5-xh+wBe!zvr6" -> 1127135251,
        "VBNU\\~1?R^9'KE<RmD}zbJ-#n/88AM<<<A=_L2u&$GJIl0{8)T#zXi6{:KwPa" -> -531520311,
        "C#aI?%qo>k@v_d_ig?LVJ&'JKcaXR[]#2o~yg{P,n:G;n:3f8NS2.MwiaGt2\";h" -> 1477673674,
        "6x:K^LuyxlFV1lDL<rhtq,+n-4(]53LsKjZY@j`/!sjEnFEa.-|Y$'eu;rvMx+bD" -> -1993549275,
        "hbx.W]b~\"t~|G\\4)B6wGEF>`{lRyZ9Nw5AVAm~/)9_Qih/(;P09Cz<nnto(hB<5OPL\"" -> 1245074680,
        "yc=P7bzo9:sr7CCf%xT0Z)amt6vOe+,[A<AyP%xFqD42C6KhNJ!Ze<5!wMNY)n}Nx@T5X" -> 1152262088,
        "8BIH&l6-vcNH3ZHlJnbd!d:yw<0a8gFCc|uGPI?2TqQ?\"Y??:U4NcTWi5W%'*hnD'gZ34i" -> 1190321287,
        "h3GauRZiF?:]%T1tC/zKR(x<)mr\"00\"8li4'niOwT@%8a)?]HYo[M-a[QyrTS<fx+Sh?g`LBP" -> -703422062,
        "RH:f^FMI5mn$GGhf9Ci\"htS/%,I'MWvqf!T~:-P4T&\\+.V`F7:52I^'&HppR?auiku,^`_xbWY" -> 495104807,
        "H]Ao84uA5cGhbHI,CdX{$Z8V=\"6GPPKn{lRKaPqKrlvq]\"^QC`?gd,d^fi\\]enSFe8?]8fMz,>.c" -> -515106956,
        "(?Z2&e`o+fr^f(rY{]5No0d9n~*+.|[c)1uy'(I,zrA*%;fdX5{7^%z=W8h4:kJ;[+X;'PRGNG~q" -> 891107636,
        "\\iUiM}yO!7a{lpMu)~)1?t0/|bai(-Chsn$pj*D9dt.=HDtS}6<A|Jc[\\\\(Kl{Cp6Her\"FzUt<kEg" -> 1449127216,
        "EJ_wHtV]1vU4h;J{UN'ql7j)\\s.w/[;U`h]\\'=u3Ahn,JP[svy%A;1+&%Va+;;Q':j=\\rZpyxDnuYd" -> 1059099969,
        "4X<>b,/XXjCgO55N\"|F^5w95[5.xpC$IOrVEn]2~h;So\\nzQ2^ljP@&=^[dr%?YFo;g51*2QV]Jxs^" -> -1717993476,
        "f-XxJ`1#Ho3rDt)bSt_2q.{r0u8gQSquV:yZHezL#gH@3hLSY,s=f1]@|i*_<H[r*](77}vpURDMm'4`" -> 2051311505,
        "]&.2'xN[\\OlAE%uREHv?a6CNF${`SjOB&}nn3K@H?]-|8[e9!nv8Gd*6ZQsMRUZds./'3\"J;w3e9`?7&" -> -1649432579,
        "njF>n;%Z0K[m}1{%`a8XHVQrpi4L{99wluivS,B]~kn)KQe@'&JU|o&#_H-FR-LgJ.wlosT{ke,$Z-oVv" -> -1229421627,
        "9[kfJLH?Fi=urks,<<?PqV99l-:10@\")69Y;g'g1F<kzlaILk\"c*%<8Zhs]_6Qfgz!^'Iz`1w.k6k@79:o" -> -1754471511,
        "Pt.g=aYcB:,5~6b2Oj7Q'uFdl*j)Rr@%2}0D1ObS`fdL[#iASDm|5i5MQHs[W=Ibu0-VOn7,'eWOlw'#uW0\\`|" -> 864890460,
        "BaE7sC9}&P[/=Y+;W<G\\wR^5Po-c{8bJ@]@AJ2AVBdNOg3-O6Ez*L-S#bW`;b%VV[iq5-z6o$wp>BDuoqLdl,9" -> 353958455,
        "biW4ZIe%y{h8kC.El'F@b4z5rZ6v&wM'u&^Q,PyHtB>bVf38Un;P\"p?6AjRC=bG}11eA-Z>r\\wL+GDh2^^fMo>^L" -> -1053915783,
        "rj;4HPW9-YB!&jZf_M/t1|M%]g^(S}<o`p:`Lzuh\"(jVjy\\uj1:!]{&J]'/6i4+o=X^)&5/CB_p^X3z`X|TVNros" -> -337284865,
        "/Yr\"FcOtQ`+[1~B-.QO_2\\%6C'r&3uN_-wG%a7wrW_\"NYw`7~&RY|!.$yqyp&^*]xFW-l+m;mtOlu073/VPQO![qg" -> -2060320272,
        "X:=ooj=-e|YA~v]r<0midQBkdC|Pe_+&9\\X;tHl?gpc.PWC%Wee2SafnLp4*LR5-cZ^>0j{'+o6y.zv&P'Tx85N#p" -> -2006453387,
        "AlckXl&^0B/u?>a5ZYcjYK\\*$^OKGBO\"vVa$!ZlB8I)k]w(JqSO.W'`+h<y\"ab9-&N\"n!Eh3g0;fIP|XBWh0UGY3I" -> -1984454530,
        "FP,)65RX(|r)ZcFj;.WjB`]L`EzGd$fRfwYf_4z@:b;r},>WL3*0=kf!ZwCt\\G&*W{2[bAR+/Tn.EE7C]Qy{utkh2_1" -> -1180356483,
        "&4sBOd:;(0mOarSi4-<P'uMC>nC8vCLuUO*\"dQVuZ^mr1h=Se#X,/8PTvM<DK1hJFj}?|%Ep/HCI4Onyk4~]kj-85tco" -> -540734744,
        "l9l#WAuH:Inj\"c9z*WRg6:fqHgmBQ2)j\"3BM`fo=?}4L2UDw%V<w<\\4}%tbq=!7@t4.@G^'D^GT:1{>#Kf1k\"c:Wz_`M" -> 2146637158,
        "/M'}rla2`IEZz^+pUK;iX<kK}gz}t>7W]latWS\\Iu;l!7'2sfAxEtH{9|U7gi(\\=/x[Z.lH+3N<V7%\\7CLGr7d+c[rp;9+" -> -234134745,
        "iQ)V~70C)^grdz;G/mfv)V4YaC:55k,,w`}gHPKa@w;IX?<m\"/yQ\"K#o{M8}sJ*<a%94b1ST~SJ-uN/jnN,J2@SiFHU1rHX" -> 2117809941,
        ".e8m|~/wHP8TABXt<aF6NB2_sbKbOSLxaU~,kffo-YWi9&Nx>:EX?SQG\"E6JF!H8jXr_rkb;(i&]f%_->\\w0<>DIkf~TOtmP" -> 549289585,
        "V4'@ctc+AL8sA7K&f,NSQxLZ>H&uZwnjAO@rUUF;Jike\"V9i9ZX3pH0gD/7y8=by5]/V&V&C<U)<Q997N9*RZ&F8\"BW{QJ`b/-" -> -2019126270,
        "UQE]h^F\"8][W*_`<88?v;PP;]H<CFLA@/yE7E/>Eaq)f/){*,u6CX)WsbXel~1l-6DbqBZwP<>.F3eF){o}Wf*fQ0k{DG].eD0" -> -81859429,
        "NS9ipvrxhD/JeNoI)7c/6(y3QS&b>=CWOkq,+2#y{OcB;9XmqSwHcC~ZNZguF-;bT$IBX@12OMcnfFF&MID,:@Vv(GMp.%KhuQ" -> -1375099416
    )
}
