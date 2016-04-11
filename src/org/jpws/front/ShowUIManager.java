/*
 *  ShowUIManager in org.jpws.front
 *  file: ShowUIManager.java
 * 
 *  Project Jpws-Front
 *  @author Wolfgang Keller
 *  Created 07.01.2006
 *  Version
 * 
 *  Copyright (c) 2006 by Wolfgang Keller, Munich, Germany
 * 
 This program is not freeware software but copyright protected to the author(s)
 stated above. However, you can use, redistribute and/or modify it under the terms 
 of the GNU General Public License as published by the Free Software Foundation, 
 version 2 of the License.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along with
 this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 Place - Suite 330, Boston, MA 02111-1307, USA, or go to
 http://www.gnu.org/copyleft/gpl.html.
 */

package org.jpws.front;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;

public class ShowUIManager 
{
public static void main(String[] args) {
try {
Set defaults = UIManager.getLookAndFeelDefaults().entrySet();
TreeSet ts = new TreeSet(new Comparator() {
public int compare(Object a, Object b) {
Map.Entry ea = (Map.Entry) a;
Map.Entry eb = (Map.Entry) b;
return ((String) ea.getKey()).compareTo(((String)
eb.getKey()));
}
});
ts.addAll(defaults);
Object[][] kvPairs = new Object[defaults.size()][2];
Object[] columnNames = new Object[] { "Key", "Value" };
int row = 0;
for (Iterator i = ts.iterator(); i.hasNext();) {
Object o = i.next();
Map.Entry entry = (Map.Entry) o;
kvPairs[row][0] = entry.getKey();
kvPairs[row][1] = entry.getValue();
row++;
}

JTable table = new JTable(kvPairs, columnNames);
JScrollPane tableScroll = new JScrollPane(table);

JButton closeButton = new JButton("Close");
closeButton.addActionListener(new ActionListener() {
public void actionPerformed(ActionEvent e) {
System.exit(0);
}
});

JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER,
6, 6));
buttons.add(closeButton, null);

JPanel main = new JPanel(new BorderLayout());
main.add(tableScroll, BorderLayout.CENTER);
main.add(buttons, BorderLayout.SOUTH);

JFrame frame = new JFrame("UI Properties");
frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
frame.getContentPane().add(main);
frame.pack();
frame.setLocationRelativeTo(null);
frame.setVisible(true);
} catch (Exception ex) {
ex.printStackTrace();
}
}
}