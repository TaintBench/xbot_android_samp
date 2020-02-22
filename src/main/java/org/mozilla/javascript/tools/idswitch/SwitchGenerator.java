package org.mozilla.javascript.tools.idswitch;

import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.tools.ToolErrorReporter;

public class SwitchGenerator {
    private CodePrinter P;
    private ToolErrorReporter R;
    private boolean c_was_defined;
    int char_tail_test_threshold = 2;
    private int[] columns;
    private String default_value;
    private IdValuePair[] pairs;
    private String source_file;
    int use_if_threshold = 3;
    String v_c = "c";
    String v_guess = "X";
    String v_id = "id";
    String v_label = "L";
    String v_length_suffix = "_length";
    String v_s = "s";
    String v_switch_label = "L0";

    public CodePrinter getCodePrinter() {
        return this.P;
    }

    public void setCodePrinter(CodePrinter value) {
        this.P = value;
    }

    public ToolErrorReporter getReporter() {
        return this.R;
    }

    public void setReporter(ToolErrorReporter value) {
        this.R = value;
    }

    public String getSourceFileName() {
        return this.source_file;
    }

    public void setSourceFileName(String value) {
        this.source_file = value;
    }

    public void generateSwitch(String[] pairs, String default_value) {
        int N = pairs.length / 2;
        IdValuePair[] id_pairs = new IdValuePair[N];
        for (int i = 0; i != N; i++) {
            id_pairs[i] = new IdValuePair(pairs[i * 2], pairs[(i * 2) + 1]);
        }
        generateSwitch(id_pairs, default_value);
    }

    public void generateSwitch(IdValuePair[] pairs, String default_value) {
        int end = pairs.length;
        if (0 != end) {
            this.pairs = pairs;
            this.default_value = default_value;
            generate_body(0, end, 2);
        }
    }

    private void generate_body(int begin, int end, int indent_level) {
        this.P.indent(indent_level);
        this.P.p(this.v_switch_label);
        this.P.p(": { ");
        this.P.p(this.v_id);
        this.P.p(" = ");
        this.P.p(this.default_value);
        this.P.p("; String ");
        this.P.p(this.v_guess);
        this.P.p(" = null;");
        this.c_was_defined = false;
        int c_def_begin = this.P.getOffset();
        this.P.p(" int ");
        this.P.p(this.v_c);
        this.P.p(';');
        int c_def_end = this.P.getOffset();
        this.P.nl();
        generate_length_switch(begin, end, indent_level + 1);
        if (!this.c_was_defined) {
            this.P.erase(c_def_begin, c_def_end);
        }
        this.P.indent(indent_level + 1);
        this.P.p("if (");
        this.P.p(this.v_guess);
        this.P.p("!=null && ");
        this.P.p(this.v_guess);
        this.P.p("!=");
        this.P.p(this.v_s);
        this.P.p(" && !");
        this.P.p(this.v_guess);
        this.P.p(".equals(");
        this.P.p(this.v_s);
        this.P.p(")) ");
        this.P.p(this.v_id);
        this.P.p(" = ");
        this.P.p(this.default_value);
        this.P.p(";");
        this.P.nl();
        this.P.indent(indent_level + 1);
        this.P.p("break ");
        this.P.p(this.v_switch_label);
        this.P.p(";");
        this.P.nl();
        this.P.line(indent_level, "}");
    }

    private void generate_length_switch(int begin, int end, int indent_level) {
        boolean use_if;
        sort_pairs(begin, end, -1);
        check_all_is_different(begin, end);
        int lengths_count = count_different_lengths(begin, end);
        this.columns = new int[this.pairs[end - 1].idLength];
        if (lengths_count <= this.use_if_threshold) {
            use_if = true;
            if (lengths_count != 1) {
                this.P.indent(indent_level);
                this.P.p("int ");
                this.P.p(this.v_s);
                this.P.p(this.v_length_suffix);
                this.P.p(" = ");
                this.P.p(this.v_s);
                this.P.p(".length();");
                this.P.nl();
            }
        } else {
            use_if = false;
            this.P.indent(indent_level);
            this.P.p(this.v_label);
            this.P.p(": switch (");
            this.P.p(this.v_s);
            this.P.p(".length()) {");
            this.P.nl();
        }
        int same_length_begin = begin;
        int cur_l = this.pairs[begin].idLength;
        int l = 0;
        int i = begin;
        while (true) {
            int next_indent;
            i++;
            if (i != end) {
                l = this.pairs[i].idLength;
                if (l == cur_l) {
                    continue;
                }
            }
            if (use_if) {
                this.P.indent(indent_level);
                if (same_length_begin != begin) {
                    this.P.p("else ");
                }
                this.P.p("if (");
                if (lengths_count == 1) {
                    this.P.p(this.v_s);
                    this.P.p(".length()==");
                } else {
                    this.P.p(this.v_s);
                    this.P.p(this.v_length_suffix);
                    this.P.p("==");
                }
                this.P.p(cur_l);
                this.P.p(") {");
                next_indent = indent_level + 1;
            } else {
                this.P.indent(indent_level);
                this.P.p("case ");
                this.P.p(cur_l);
                this.P.p(":");
                next_indent = indent_level + 1;
            }
            generate_letter_switch(same_length_begin, i, next_indent, !use_if, use_if);
            if (use_if) {
                this.P.p("}");
                this.P.nl();
            } else {
                this.P.p("break ");
                this.P.p(this.v_label);
                this.P.p(";");
                this.P.nl();
            }
            if (i == end) {
                break;
            }
            same_length_begin = i;
            cur_l = l;
        }
        if (!use_if) {
            this.P.indent(indent_level);
            this.P.p("}");
            this.P.nl();
        }
    }

    private void generate_letter_switch(int begin, int end, int indent_level, boolean label_was_defined, boolean inside_if) {
        int L = this.pairs[begin].idLength;
        for (int i = 0; i != L; i++) {
            this.columns[i] = i;
        }
        generate_letter_switch_r(begin, end, L, indent_level, label_was_defined, inside_if);
    }

    private boolean generate_letter_switch_r(int begin, int end, int L, int indent_level, boolean label_was_defined, boolean inside_if) {
        boolean next_is_unreachable = false;
        int i;
        if (begin + 1 == end) {
            this.P.p(' ');
            IdValuePair pair = this.pairs[begin];
            if (L > this.char_tail_test_threshold) {
                this.P.p(this.v_guess);
                this.P.p("=");
                this.P.qstring(pair.id);
                this.P.p(";");
                this.P.p(this.v_id);
                this.P.p("=");
                this.P.p(pair.value);
                this.P.p(";");
            } else if (L == 0) {
                next_is_unreachable = true;
                this.P.p(this.v_id);
                this.P.p("=");
                this.P.p(pair.value);
                this.P.p("; break ");
                this.P.p(this.v_switch_label);
                this.P.p(";");
            } else {
                this.P.p("if (");
                int column = this.columns[0];
                this.P.p(this.v_s);
                this.P.p(".charAt(");
                this.P.p(column);
                this.P.p(")==");
                this.P.qchar(pair.id.charAt(column));
                for (i = 1; i != L; i++) {
                    this.P.p(" && ");
                    column = this.columns[i];
                    this.P.p(this.v_s);
                    this.P.p(".charAt(");
                    this.P.p(column);
                    this.P.p(")==");
                    this.P.qchar(pair.id.charAt(column));
                }
                this.P.p(") {");
                this.P.p(this.v_id);
                this.P.p("=");
                this.P.p(pair.value);
                this.P.p("; break ");
                this.P.p(this.v_switch_label);
                this.P.p(";}");
            }
            this.P.p(' ');
            return next_is_unreachable;
        }
        boolean use_if;
        int max_column_index = find_max_different_column(begin, end, L);
        int max_column = this.columns[max_column_index];
        int count = count_different_chars(begin, end, max_column);
        this.columns[max_column_index] = this.columns[L - 1];
        if (inside_if) {
            this.P.nl();
            this.P.indent(indent_level);
        } else {
            this.P.p(' ');
        }
        if (count <= this.use_if_threshold) {
            use_if = true;
            this.c_was_defined = true;
            this.P.p(this.v_c);
            this.P.p("=");
            this.P.p(this.v_s);
            this.P.p(".charAt(");
            this.P.p(max_column);
            this.P.p(");");
        } else {
            use_if = false;
            if (!label_was_defined) {
                label_was_defined = true;
                this.P.p(this.v_label);
                this.P.p(": ");
            }
            this.P.p("switch (");
            this.P.p(this.v_s);
            this.P.p(".charAt(");
            this.P.p(max_column);
            this.P.p(")) {");
        }
        int same_char_begin = begin;
        int cur_ch = this.pairs[begin].id.charAt(max_column);
        int ch = 0;
        i = begin;
        while (true) {
            int next_indent;
            i++;
            if (i != end) {
                ch = this.pairs[i].id.charAt(max_column);
                if (ch == cur_ch) {
                    continue;
                }
            }
            if (use_if) {
                this.P.nl();
                this.P.indent(indent_level);
                if (same_char_begin != begin) {
                    this.P.p("else ");
                }
                this.P.p("if (");
                this.P.p(this.v_c);
                this.P.p("==");
                this.P.qchar(cur_ch);
                this.P.p(") {");
                next_indent = indent_level + 1;
            } else {
                this.P.nl();
                this.P.indent(indent_level);
                this.P.p("case ");
                this.P.qchar(cur_ch);
                this.P.p(":");
                next_indent = indent_level + 1;
            }
            boolean after_unreachable = generate_letter_switch_r(same_char_begin, i, L - 1, next_indent, label_was_defined, use_if);
            if (use_if) {
                this.P.p("}");
            } else if (!after_unreachable) {
                this.P.p("break ");
                this.P.p(this.v_label);
                this.P.p(";");
            }
            if (i == end) {
                break;
            }
            same_char_begin = i;
            cur_ch = ch;
        }
        if (use_if) {
            this.P.nl();
            if (inside_if) {
                this.P.indent(indent_level - 1);
            } else {
                this.P.indent(indent_level);
            }
        } else {
            this.P.nl();
            this.P.indent(indent_level);
            this.P.p("}");
            if (inside_if) {
                this.P.nl();
                this.P.indent(indent_level - 1);
            } else {
                this.P.p(' ');
            }
        }
        this.columns[max_column_index] = max_column;
        return 0;
    }

    private int count_different_lengths(int begin, int end) {
        int lengths_count = 0;
        int cur_l = -1;
        while (begin != end) {
            int l = this.pairs[begin].idLength;
            if (cur_l != l) {
                lengths_count++;
                cur_l = l;
            }
            begin++;
        }
        return lengths_count;
    }

    private int find_max_different_column(int begin, int end, int L) {
        int max_count = 0;
        int max_index = 0;
        for (int i = 0; i != L; i++) {
            int column = this.columns[i];
            sort_pairs(begin, end, column);
            int count = count_different_chars(begin, end, column);
            if (count == end - begin) {
                return i;
            }
            if (max_count < count) {
                max_count = count;
                max_index = i;
            }
        }
        if (max_index != L - 1) {
            sort_pairs(begin, end, this.columns[max_index]);
        }
        return max_index;
    }

    private int count_different_chars(int begin, int end, int column) {
        int chars_count = 0;
        int cur_ch = -1;
        while (begin != end) {
            int ch = this.pairs[begin].id.charAt(column);
            if (ch != cur_ch) {
                chars_count++;
                cur_ch = ch;
            }
            begin++;
        }
        return chars_count;
    }

    private void check_all_is_different(int begin, int end) {
        if (begin != end) {
            IdValuePair prev = this.pairs[begin];
            while (true) {
                begin++;
                if (begin != end) {
                    IdValuePair current = this.pairs[begin];
                    if (prev.id.equals(current.id)) {
                        throw on_same_pair_fail(prev, current);
                    }
                    prev = current;
                } else {
                    return;
                }
            }
        }
    }

    private EvaluatorException on_same_pair_fail(IdValuePair a, IdValuePair b) {
        int line1 = a.getLineNumber();
        int line2 = b.getLineNumber();
        if (line2 > line1) {
            int tmp = line1;
            line1 = line2;
            line2 = tmp;
        }
        return this.R.runtimeError(ToolErrorReporter.getMessage("msg.idswitch.same_string", a.id, new Integer(line2)), this.source_file, line1, null, 0);
    }

    private void sort_pairs(int begin, int end, int comparator) {
        heap4Sort(this.pairs, begin, end - begin, comparator);
    }

    private static boolean bigger(IdValuePair a, IdValuePair b, int comparator) {
        if (comparator < 0) {
            int diff = a.idLength - b.idLength;
            if (diff != 0) {
                if (diff > 0) {
                    return true;
                }
                return false;
            } else if (a.id.compareTo(b.id) <= 0) {
                return false;
            } else {
                return true;
            }
        } else if (a.id.charAt(comparator) <= b.id.charAt(comparator)) {
            return false;
        } else {
            return true;
        }
    }

    private static void heap4Sort(IdValuePair[] array, int offset, int size, int comparator) {
        if (size > 1) {
            makeHeap4(array, offset, size, comparator);
            while (size > 1) {
                size--;
                IdValuePair v1 = array[offset + size];
                array[offset + size] = array[offset + 0];
                array[offset + 0] = v1;
                heapify4(array, offset, size, 0, comparator);
            }
        }
    }

    private static void makeHeap4(IdValuePair[] array, int offset, int size, int comparator) {
        int i = (size + 2) >> 2;
        while (i != 0) {
            i--;
            heapify4(array, offset, size, i, comparator);
        }
    }

    private static void heapify4(IdValuePair[] array, int offset, int size, int i, int comparator) {
        IdValuePair i_val = array[offset + i];
        while (true) {
            int base = i << 2;
            int new_i1 = base | 1;
            int new_i2 = base | 2;
            int new_i3 = base | 3;
            int new_i4 = base + 4;
            IdValuePair val1;
            IdValuePair val2;
            IdValuePair val3;
            if (new_i4 < size) {
                val1 = array[offset + new_i1];
                val2 = array[offset + new_i2];
                val3 = array[offset + new_i3];
                IdValuePair val4 = array[offset + new_i4];
                if (bigger(val2, val1, comparator)) {
                    val1 = val2;
                    new_i1 = new_i2;
                }
                if (bigger(val4, val3, comparator)) {
                    val3 = val4;
                    new_i3 = new_i4;
                }
                if (bigger(val3, val1, comparator)) {
                    val1 = val3;
                    new_i1 = new_i3;
                }
                if (!bigger(i_val, val1, comparator)) {
                    array[offset + i] = val1;
                    array[offset + new_i1] = i_val;
                    i = new_i1;
                } else {
                    return;
                }
            } else if (new_i1 < size) {
                val1 = array[offset + new_i1];
                if (new_i2 != size) {
                    val2 = array[offset + new_i2];
                    if (bigger(val2, val1, comparator)) {
                        val1 = val2;
                        new_i1 = new_i2;
                    }
                    if (new_i3 != size) {
                        val3 = array[offset + new_i3];
                        if (bigger(val3, val1, comparator)) {
                            val1 = val3;
                            new_i1 = new_i3;
                        }
                    }
                }
                if (bigger(val1, i_val, comparator)) {
                    array[offset + i] = val1;
                    array[offset + new_i1] = i_val;
                    return;
                }
                return;
            } else {
                return;
            }
        }
    }
}
