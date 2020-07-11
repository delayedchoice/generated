# START:new
tmux new-session -s generator -n editor -d
# END:new
# START:cd
# END:cd
#tmux send-keys -t generator 'cd ~/devproject' C-m
# START:vim
tmux send-keys -t generator 'n src/generator/core.clj' C-m
# END:vim
# START:hsplit
#tmux split-window -v -t generator
#tmux selectp -t 2 
## END:hsplit
## START:layout
#tmux select-layout -t generator main-horizontal
# END:layout
# START:keystopane
#tmux send-keys -t generator:1.2 'cd ~/devproject' C-m
# END:keystopane
# START:newwindow
tmux new-window -n console -t generator
tmux send-keys -t generator 'lein repl' C-m
#tmux send-keys -t generator:2 'cd ~/devproject' C-m
# END:newwindow
# START:selectwindow
tmux select-window -t generator:1
# END:selectwidow
tmux attach -t generator
