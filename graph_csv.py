
import csv
import matplotlib.ticker as ticker
import matplotlib.pyplot as plt
import matplotlib
import numpy as np
import pandas as pd
import argparse
import os
import sys

RELOAD_FREQ_SECONDS = 4
COLOR_PALETTE = 'tab20'

def on_close(event):
    sys.exit(0)

def fetch_param_info(test_folder, test_id):
    param_info = pd.read_csv(os.path.join(test_folder, test_id + ".info.csv"))
    return param_info

def fetch_data(test_folder, test_id):
    df = pd.read_csv(os.path.join(test_folder, test_id + ".csv"))
    return df

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--test-folder', dest='test_folder', type=str, required=True, help='Path to the folder containing .csv files')
    parser.add_argument('--test-id', dest='test_id', required=True, type=str, help='ID of the test to process')
    group = parser.add_mutually_exclusive_group()
    group.add_argument('--start-iter-zero', dest='start_iter_zero', default=False, action='store_true', help='If true, display the graph as starting from iteration 0 instead of from the first recorded iteration')
    group.add_argument('--treat-first-iter-zero', dest='treat_first_iter_zero', action='store_true', help='If true, and if --start-iter-zero is false, treat the values of the first recorded iteration as if they were the start values')
    args = parser.parse_args()

    param_info = fetch_param_info(args.test_folder, args.test_id)
    start_vals = param_info['start'].tolist()
    step_vals = param_info['step'].tolist()

    plt.style.use('dark_background')
    fig, ax = plt.subplots(constrained_layout=True)
    fig.set_figheight(8)
    fig.set_figwidth(10)
    fig.canvas.mpl_connect('close_event', on_close)
    plt.ion()

    instName = os.path.basename(os.path.normpath(args.test_folder))
    link = f'{instName}/tune/{args.test_id}/'
    fig.suptitle(link)

    firstDraw = True

    while True:
        data = fetch_data(args.test_folder, args.test_id)

        first_iter = data['iteration'][0]
        if (first_iter != 0):
            if args.start_iter_zero:
                data.loc[-1] = [0] + start_vals
                data.index = data.index + 1
                data.sort_index(inplace=True)
            elif args.treat_first_iter_zero:
                row1 = data.iloc[0].tolist()
                start_vals = row1[1:]

        delta_data = data.copy()
        for col in data.columns:
            if col != 'iteration':
                col_idx = data.columns.get_loc(col) - 1
                delta_data[col] = (data[col] - start_vals[col_idx]) / step_vals[col_idx]

        if not firstDraw:
            xlim = ax.get_xlim()
            ylim = ax.get_ylim()

        ax.clear()
        
        if not firstDraw:
            ax.set_xlim(xlim)
            ax.set_ylim(ylim)
        
        firstDraw = False

        num_params = len(start_vals)
        cmap = plt.get_cmap(COLOR_PALETTE)

        plt.xlabel('Iteration')
        plt.ylabel('Delta as % of stepsize')
        ax.set_prop_cycle(color=[cmap(1.*i/num_params) for i in range(num_params)])

        numeric_cols = delta_data.select_dtypes(include=['number']).columns.drop('iteration')
        x = delta_data['iteration']
        for col in numeric_cols:
            plt.plot(x, delta_data[col], label=col, linewidth=1)

        num_cols = 1 + (num_params // 25)
        plt.legend(loc='center left', bbox_to_anchor=(1, 0.5), ncol=num_cols)

        plt.pause(RELOAD_FREQ_SECONDS)


if __name__ == '__main__':
    main()
